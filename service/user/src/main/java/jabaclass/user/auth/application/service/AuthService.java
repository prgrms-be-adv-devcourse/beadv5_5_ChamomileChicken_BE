package jabaclass.user.auth.application.service;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.nio.charset.StandardCharsets;

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.jsonwebtoken.Claims;

import jakarta.annotation.PostConstruct;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import jabaclass.user.auth.application.usecase.TokenStatusUseCase;
import jabaclass.user.auth.domain.model.TokenBlacklist;
import jabaclass.user.auth.presentation.dto.response.TokenStatusResult;
import jabaclass.user.auth.domain.repository.TokenBlacklistRepository;
import jabaclass.user.user.domain.model.SocialType;
import jabaclass.user.user.domain.model.UserRole;
import jabaclass.user.auth.infrastructure.jwt.JwtProvider;
import jabaclass.user.auth.application.exception.AuthErrorCode;
import jabaclass.user.auth.application.exception.AuthException;
import jabaclass.user.auth.application.usecase.LoginUseCase;
import jabaclass.user.auth.application.usecase.LogoutUseCase;
import jabaclass.user.auth.application.usecase.ReissueUseCase;
import jabaclass.user.auth.infrastructure.jwt.TokenProvider;
import jabaclass.user.auth.presentation.dto.request.LoginRequestDto;
import jabaclass.user.auth.presentation.dto.response.TokenResult;
import jabaclass.user.user.domain.model.User;
import jabaclass.user.user.domain.repository.UserRepository;
import jabaclass.user.auth.application.usecase.ReportTheftUseCase;
import jabaclass.user.mail.application.service.MailService;

@Transactional(readOnly = true)
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService
    implements LoginUseCase, LogoutUseCase, ReissueUseCase, ReportTheftUseCase, TokenStatusUseCase {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenProvider tokenProvider;
    private final JwtProvider jwtProvider;
    private final MailService mailService;
    private final TokenBlacklistRepository tokenBlacklistRepository;
    private final CircuitBreakerRegistry circuitBreakerRegistry;
    private final LoginWriter loginWriter;
    private CircuitBreaker redisReadCb;
    private CircuitBreaker redisWriteCb;

    private static final String BLACKLIST_PREFIX = "blacklist:";
    private static final String FORCE_LOGOUT_PREFIX = "force_logout:";
    private static final String THEFT_REPORT_PREFIX = "theft_report:";
    private final StringRedisTemplate redisTemplate;

    @Value("${jwt.refresh-token-validity}")
    private long refreshTokenValidity;

    @Value("${jwt.access-token-validity}")
    private long accessTokenValidity;

    @Value("${app.base-url}")
    private String baseUrl;

    @PostConstruct
    void init() {
        this.redisReadCb = circuitBreakerRegistry.circuitBreaker("redis-read", "redis-read");
        this.redisWriteCb = circuitBreakerRegistry.circuitBreaker("redis-write", "redis-write");
    }

    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public TokenResult login(LoginRequestDto request, String clientIp, String userAgent) {

        // 1. TX1(readOnly): user 조회 후 커넥션 반납 — Spring Data JPA 자체 TX
        User user = userRepository.findByEmailAndSocialType(request.getEmail(), SocialType.SYSTEM)
            .orElseThrow(() -> new AuthException(AuthErrorCode.USER_NOT_FOUND));

        // 2. bcrypt — DB 커넥션 없음 (~100-300ms)
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new AuthException(AuthErrorCode.USER_NOT_FOUND);
        }

        // 3. 새 기기 판별 — in-memory (commitLoginWrites 이전, 변경 전 lastLoginIp 기준)
        boolean isNewDevice = user.getLastLoginIp() != null &&
            (!clientIp.equals(user.getLastLoginIp()) || !userAgent.equals(user.getLastLoginUserAgent()));

        // 4. 토큰 생성 — I/O 없음
        String accessToken = tokenProvider.generateAccessToken(user.getId(), user.getRole());
        String refreshToken = tokenProvider.generateRefreshToken(user.getId(), user.getRole());

        // 5. TX2(write): DB에 refreshToken + lastLogin 저장 — LoginWriter 프록시를 통해 별도 TX
        loginWriter.commitLoginWrites(user.getId(), refreshToken, clientIp, userAgent);

        // 6. Redis에 refreshToken 저장 (DB 커밋 이후)
        executeWriteWithCb(() -> redisTemplate.opsForValue().set(
            "refresh:" + user.getId(), refreshToken, Duration.ofMillis(refreshTokenValidity)
        ), "refresh:" + user.getId());

        // 7. 새 기기 보안 알림 — DB 커밋 성공 후 발송
        sendNewDeviceAlertIfNeeded(isNewDevice, user.getId(), user.getEmail(), user.getName(), clientIp, userAgent);

        log.info("[AUTH] 로그인 성공. userId={}, ip={}", user.getId(), clientIp);
        return new TokenResult(accessToken, refreshToken);
    }

    @Transactional(noRollbackFor = AuthException.class)
    @Override
    public TokenResult reissue(String refreshToken) {
        Claims claims = jwtProvider.parseClaims(refreshToken);

        if (!jwtProvider.isRefreshToken(claims)) {
            throw new AuthException(AuthErrorCode.INVALID_REFRESH_TOKEN);
        }

        UUID userId = jwtProvider.getUserId(claims);
        String role = jwtProvider.getRole(claims);

        // Step 1: stored RT 조회 — Redis 우선, 실패 시 DB
        // user: Redis miss 시 이미 로드됨. Redis hit 시 null (이후 필요한 시점에 단 1회 로드)
        String stored = loadFromRedis(userId);
        User user = null;

        if (stored == null) {
            user = userRepository.findById(userId)
                .orElseThrow(() -> new AuthException(AuthErrorCode.USER_NOT_FOUND));
            if (user.getRefreshToken() == null) {
                throw new AuthException(AuthErrorCode.ALREADY_LOGGED_OUT);
            }
            stored = user.getRefreshToken();
        }

        // Step 2: RTR 검증
        if (!stored.equals(refreshToken)) {
            if (user == null) {
                user = userRepository.findById(userId)
                    .orElseThrow(() -> new AuthException(AuthErrorCode.USER_NOT_FOUND));
            }
            user.forceLogout();
            user.updateRefreshToken(null);
            executeWriteWithCb(() -> redisTemplate.opsForValue().set(
                FORCE_LOGOUT_PREFIX + userId,
                String.valueOf(Instant.now().toEpochMilli()),
                Duration.ofMillis(accessTokenValidity)
            ), FORCE_LOGOUT_PREFIX + userId);
            executeWriteWithCb(() -> redisTemplate.delete("refresh:" + userId), "refresh:" + userId);
            log.warn("[AUTH] RTR 재사용 감지 - force_logout 설정. userId={}", userId);
            throw new AuthException(AuthErrorCode.SUSPECTED_TOKEN_THEFT);
        }

        // Step 3: 새 토큰 발급
        UserRole userRole;
        try {
            userRole = UserRole.valueOf(role);
        } catch (IllegalArgumentException e) {
            throw new AuthException(AuthErrorCode.INVALID_TOKEN);
        }

        String newAccessToken = tokenProvider.generateAccessToken(userId, userRole);
        String newRefreshToken = tokenProvider.generateRefreshToken(userId, userRole);

        // Step 4: DB 업데이트 (Redis hit 경로: 이 시점에 1번만 조회)
        if (user == null) {
            user = userRepository.findById(userId)
                .orElseThrow(() -> new AuthException(AuthErrorCode.USER_NOT_FOUND));
        }
        user.updateRefreshToken(newRefreshToken);
        executeWriteWithCb(() -> redisTemplate.opsForValue().set(
            "refresh:" + userId, newRefreshToken, Duration.ofMillis(refreshTokenValidity)
        ), "refresh:" + userId);

        return new TokenResult(newAccessToken, newRefreshToken);
    }

    @CacheEvict(cacheNames = "tokenStatus", key = "#accessToken")
    @Override
    @Transactional
    public void logout(UUID userId, String accessToken) {

        User user = userRepository.findById(userId)
            .orElseThrow(() -> new AuthException(AuthErrorCode.USER_NOT_FOUND));
        user.updateRefreshToken(null);

        executeWriteWithCb(() -> redisTemplate.delete("refresh:" + userId), "refresh:" + userId);

        try {
            Claims claims = jwtProvider.parseClaims(accessToken);
            long remainingMillis = claims.getExpiration().getTime() - System.currentTimeMillis();

            if (remainingMillis > 0) {
                String hash = sha256(accessToken);
                LocalDateTime expiresAt = LocalDateTime.ofInstant(
                    Instant.ofEpochMilli(claims.getExpiration().getTime()), ZoneOffset.UTC);
                tokenBlacklistRepository.save(TokenBlacklist.of(hash, expiresAt));

                log.info("[AUTH] Blacklist DB 등록. ttl={}ms", remainingMillis);

                executeWriteWithCb(() -> redisTemplate.opsForValue().set(
                    BLACKLIST_PREFIX + accessToken, "logout", Duration.ofMillis(remainingMillis)
                ), BLACKLIST_PREFIX + "***");
            } else {
                log.warn("[AUTH] 토큰 이미 만료. remainingMillis={}", remainingMillis);
            }
        } catch (Exception e) {
            log.warn("[AUTH] 액세스 토큰 파싱 실패, 블랙리스트 미등록. reason={}", e.getMessage());
        }
    }

    @CacheEvict(cacheNames = "tokenStatus", allEntries = true)
    @Override
    @Transactional
    public void reportTheft(String token) {

        String userIdStr;
        try {
            userIdStr = redisReadCb.executeCallable(
                () -> redisTemplate.opsForValue().get(THEFT_REPORT_PREFIX + token)
            );
        } catch (CallNotPermittedException e) {
            log.warn("[AUTH] Redis read CB OPEN, theft_report 조회 불가.");
            throw new AuthException(AuthErrorCode.THEFT_REPORT_TOKEN_EXPIRED);
        } catch (Exception e) {
            log.warn("[AUTH] Redis 장애, theft_report 조회 실패. 만료 처리.");
            throw new AuthException(AuthErrorCode.THEFT_REPORT_TOKEN_EXPIRED);
        }

        if (userIdStr == null) {
            throw new AuthException(AuthErrorCode.THEFT_REPORT_TOKEN_EXPIRED);
        }

        UUID userId = UUID.fromString(userIdStr);
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new AuthException(AuthErrorCode.USER_NOT_FOUND));
        user.forceLogout();
        user.updateRefreshToken(null);

        executeWriteWithCb(() -> redisTemplate.opsForValue().set(
            FORCE_LOGOUT_PREFIX + userId,
            String.valueOf(Instant.now().toEpochMilli()),
            Duration.ofMillis(accessTokenValidity)
        ), FORCE_LOGOUT_PREFIX + userId);

        executeWriteWithCb(() -> redisTemplate.delete("refresh:" + userId), "refresh:" + userId);
        executeWriteWithCb(() -> redisTemplate.delete(THEFT_REPORT_PREFIX + token), THEFT_REPORT_PREFIX + "***");

        log.warn("[AUTH] 본인 아님 신고 처리 완료. userId={}", userId);
    }

    @Override
    @Cacheable(cacheNames = "tokenStatus", key = "#token")
    public TokenStatusResult checkTokenStatus(String token, UUID userId, long tokenIssuedAtMillis) {
        String hash = sha256(token);
        if (tokenBlacklistRepository.existsByTokenHashAndExpiresAtAfter(hash, LocalDateTime.now(ZoneOffset.UTC))) {
            return TokenStatusResult.blacklisted();
        }

        User user = userRepository.findById(userId).orElse(null);
        if (user != null && user.getForceLogoutAt() != null) {
            long forceLogoutMillis = user.getForceLogoutAt()
                .toInstant(ZoneOffset.UTC).toEpochMilli();
            if (tokenIssuedAtMillis <= forceLogoutMillis) {
                return TokenStatusResult.forceLogout();
            }
        }

        return TokenStatusResult.valid();
    }

    @Transactional
    public TokenResult issueOAuth2Tokens(UUID userId, UserRole role, String clientIp, String userAgent) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new AuthException(AuthErrorCode.USER_NOT_FOUND));

        // 변경 전 lastLoginIp 기준으로 판별
        boolean isNewDevice = user.getLastLoginIp() != null &&
            (!clientIp.equals(user.getLastLoginIp()) || !userAgent.equals(user.getLastLoginUserAgent()));

        user.updateLastLogin(clientIp, userAgent);

        String accessToken = tokenProvider.generateAccessToken(userId, role);
        String refreshToken = tokenProvider.generateRefreshToken(userId, role);

        user.updateRefreshToken(refreshToken);
        executeWriteWithCb(() -> redisTemplate.opsForValue().set(
            "refresh:" + userId, refreshToken, Duration.ofMillis(refreshTokenValidity)
        ), "refresh:" + userId);

        log.info("[AUTH] OAuth2 로그인 성공. userId={}, ip={}", userId, clientIp);

        // 모든 write 완료 후 알림 발송
        sendNewDeviceAlertIfNeeded(isNewDevice, userId, user.getEmail(), user.getName(), clientIp, userAgent);

        return new TokenResult(accessToken, refreshToken);
    }

    private String loadFromRedis(UUID userId) {
        try {
            return redisReadCb.executeCallable(
                () -> redisTemplate.opsForValue().get("refresh:" + userId)
            );
        } catch (CallNotPermittedException e) {
            log.warn("[AUTH] Redis read CB OPEN, DB fallback. userId={}", userId);
            return null;
        } catch (Exception e) {
            log.warn("[AUTH] Redis 장애, DB fallback. userId={}", userId);
            return null;
        }
    }

    private void sendNewDeviceAlertIfNeeded(boolean isNewDevice, UUID userId, String email, String name,
            String clientIp, String userAgent) {
        if (!isNewDevice) return;
        log.warn("[AUTH] 새 기기 로그인 감지. userId={}, ip={}", userId, clientIp);
        String theftReportToken = UUID.randomUUID().toString();
        try {
            redisWriteCb.executeRunnable(() -> redisTemplate.opsForValue().set(
                THEFT_REPORT_PREFIX + theftReportToken,
                userId.toString(),
                Duration.ofMillis(refreshTokenValidity)
            ));
            sendSecurityAlertAsync(email, name, clientIp, userAgent, theftReportToken);
        } catch (CallNotPermittedException e) {
            log.warn("[AUTH] Redis write CB OPEN, 새 기기 보안 알림 스킵. userId={}", userId);
        } catch (Exception e) {
            log.warn("[AUTH] 새 기기 보안 알림 등록 실패, 로그인 계속 진행. userId={}", userId);
        }
    }

    private void sendSecurityAlertAsync(String email, String name, String ip, String userAgent, String token) {
        CompletableFuture.runAsync(() -> {
            try {
                String link = baseUrl + "/api/v1/auth/report-theft?token=" + token;
                String body = createSecurityAlertEmailBody(name, ip, userAgent, link);
                mailService.send(email, "[보안 알림] 새로운 기기에서 로그인되었습니다.", body, true);
            } catch (Exception e) {
                log.error("[AUTH] 보안 알림 이메일 발송 실패. email={}", email, e);
            }
        });
    }

    private String createSecurityAlertEmailBody(String name, String ip, String userAgent, String link) {
        return """
          <div style="margin:0;padding:0;background-color:#f3f6fb;">
            <div style="width:100%%;background-color:#f3f6fb;padding:40px 16px;">
              <div style="max-width:560px;margin:0 auto;background:#ffffff;border:1px solid #e5e7eb;border-radius:20px;overflow:hidden;box-shadow:0 10px 30px rgba(15,23,42,0.08);">
                <div style="background:linear-gradient(135deg,#7f1d1d 0%%,#991b1b 100%%);padding:36px 32px 28px;text-align:left;">
                  <div style="display:inline-block;padding:6px 12px;background:rgba(255,255,255,0.15);border-radius:999px;font-size:12px;font-weight:700;letter-spacing:0.4px;color:#fecaca;margin-bottom:16px;">
                    SECURITY ALERT
                  </div>
                  <h1 style="margin:0;font-size:30px;line-height:1.3;font-weight:800;color:#ffffff;">
                    새로운 기기에서<br>로그인이 감지되었습니다
                  </h1>
                  <p style="margin:12px 0 0;font-size:15px;line-height:1.7;color:#fca5a5;">
                    본인이 아닌 경우 즉시 계정을 보호하세요.
                  </p>
                </div>

                <div style="padding:36px 32px 32px;">
                  <p style="margin:0 0 14px;font-size:16px;line-height:1.7;color:#374151;">
                    안녕하세요, <strong style="color:#111827;">%s</strong>님.
                  </p>
                  <p style="margin:0 0 28px;font-size:15px;line-height:1.7;color:#4b5563;">
                    회원님의 계정에 새로운 기기 또는 위치에서 로그인이 감지되었습니다.
                  </p>

                  <div style="margin:0 0 28px;padding:24px;background:linear-gradient(180deg,#fff7f7 0%%,#fff1f1 100%%);border:1px solid #fecaca;border-radius:18px;">
                    <div style="font-size:13px;font-weight:700;letter-spacing:0.2px;color:#991b1b;margin-bottom:16px;">
                      로그인 정보
                    </div>
                    <table style="width:100%%;border-collapse:collapse;">
                      <tr>
                        <td style="padding:8px 0;font-size:14px;color:#6b7280;width:80px;">IP 주소</td>
                        <td style="padding:8px 0;font-size:14px;font-weight:600;color:#111827;">%s</td>
                      </tr>
                      <tr style="border-top:1px solid #fee2e2;">
                        <td style="padding:8px 0;font-size:14px;color:#6b7280;">기기 정보</td>
                        <td style="padding:8px 0;font-size:13px;font-weight:500;color:#374151;word-break:break-all;">%s</td>
                      </tr>
                    </table>
                  </div>

                  <div style="text-align:center;margin:0 0 28px;">
                    <a href="%s" style="display:inline-block;padding:16px 36px;background:linear-gradient(135deg,#dc2626 0%%,#b91c1c 100%%);
                    color:#ffffff;text-decoration:none;border-radius:14px;font-size:16px;font-weight:700;letter-spacing:0.2px;box-shadow:0 4px 14px rgba(220,38,38,0.35);">
                      본인 아님 — 계정 보호하기
                    </a>
                  </div>

                  <div style="padding:18px 20px;background:#f9fafb;border:1px solid #e5e7eb;border-radius:14px;">
                    <p style="margin:0 0 8px;font-size:14px;line-height:1.6;color:#374151;">
                      이 링크는 <strong>일주일</strong> 동안 유효합니다.
                    </p>
                    <p style="margin:0;font-size:14px;line-height:1.6;color:#6b7280;">
                      본인이 로그인한 경우 이 메일을 무시하셔도 됩니다.
                    </p>
                  </div>
                </div>

                <div style="padding:20px 32px;background:#fafafa;border-top:1px solid #e5e7eb;">
                  <p style="margin:0;font-size:12px;line-height:1.6;color:#9ca3af;">
                    © Jaba Class. All rights reserved.
                  </p>
                </div>

              </div>
            </div>
          </div>
          """.formatted(name, ip, userAgent, link);
    }

    private static String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) hex.append(String.format("%02x", b));
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    private void executeWriteWithCb(Runnable action, String keyHint) {
        try {
            redisWriteCb.executeRunnable(action);
        } catch (CallNotPermittedException e) {
            log.warn("[AUTH] Redis write CB OPEN, 스킵. key={}", keyHint);
        } catch (Exception e) {
            log.warn("[AUTH] Redis write 실패, 무시. key={}", keyHint);
        }
    }
}
