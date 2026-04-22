package jabaclass.user.auth.application.service;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import io.jsonwebtoken.Claims;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
public class AuthService implements LoginUseCase, LogoutUseCase, ReissueUseCase, ReportTheftUseCase {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenProvider tokenProvider;
    private final JwtProvider jwtProvider;
    private final MailService mailService;

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

    @Override
    @Transactional
    public TokenResult login(LoginRequestDto request, String clientIp, String userAgent) {

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new AuthException(AuthErrorCode.USER_NOT_FOUND));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new AuthException(AuthErrorCode.USER_NOT_FOUND);
        }

        boolean isNewDevice = user.getLastLoginIp() != null &&
            (!clientIp.equals(user.getLastLoginIp()) || !userAgent.equals(user.getLastLoginUserAgent()));

        if (isNewDevice) {
            log.warn("[AUTH] 새 기기 로그인 감지. userId={}, ip={}", user.getId(), clientIp);
            String theftReportToken = UUID.randomUUID().toString();
            redisTemplate.opsForValue().set(
                THEFT_REPORT_PREFIX + theftReportToken,
                user.getId().toString(),
                Duration.ofMinutes(10)
            );
            sendSecurityAlertAsync(user.getEmail(), user.getName(), clientIp, userAgent, theftReportToken);
        }

        user.updateLastLogin(clientIp, userAgent);
        log.info("[AUTH] 로그인 성공. userId={}, ip={}", user.getId(), clientIp);

        String accessToken = tokenProvider.generateAccessToken(user.getId(), user.getRole());
        String refreshToken = tokenProvider.generateRefreshToken(user.getId(), user.getRole());

        redisTemplate.opsForValue().set(
            "refresh:" + user.getId(),
            refreshToken,
            Duration.ofMillis(refreshTokenValidity)
        );

        return new TokenResult(accessToken, refreshToken);
    }

    @Override
    public TokenResult reissue(String refreshToken) {
        Claims claims = jwtProvider.parseClaims(refreshToken);

        if (!jwtProvider.isRefreshToken(claims)) {
            throw new AuthException(AuthErrorCode.INVALID_REFRESH_TOKEN);
        }

        UUID userId = jwtProvider.getUserId(claims);
        String role = jwtProvider.getRole(claims);

        String stored = redisTemplate.opsForValue().get("refresh:" + userId);

        if (stored == null) {
            throw new AuthException(AuthErrorCode.ALREADY_LOGGED_OUT);
        }

        if (!stored.equals(refreshToken)) {
            redisTemplate.opsForValue().set(
                FORCE_LOGOUT_PREFIX + userId,
                String.valueOf(Instant.now().toEpochMilli()),
                Duration.ofMillis(accessTokenValidity)
            );
            redisTemplate.delete("refresh:" + userId);
            log.warn("[AUTH] RTR 재사용 감지 - force_logout 설정. userId={}", userId);
            throw new AuthException(AuthErrorCode.SUSPECTED_TOKEN_THEFT);
        }

        UserRole userRole;
        try {
            userRole = UserRole.valueOf(role);
        } catch (IllegalArgumentException e) {
            throw new AuthException(AuthErrorCode.INVALID_TOKEN);
        }

        String newAccessToken = tokenProvider.generateAccessToken(userId, userRole);
        String newRefreshToken = tokenProvider.generateRefreshToken(userId, userRole);

        redisTemplate.opsForValue().set("refresh:" + userId, newRefreshToken,
            Duration.ofMillis(refreshTokenValidity));

        return new TokenResult(newAccessToken, newRefreshToken);
    }

    @Override
    public void logout(UUID userId, String accessToken) {
        redisTemplate.delete("refresh:" + userId);

        try {
            Claims claims = jwtProvider.parseClaims(accessToken);
            long remainingMillis = claims.getExpiration().getTime() - System.currentTimeMillis();

            if (remainingMillis > 0) {
                log.info("[AUTH] Blacklist 등록. key={}, ttl={}ms", BLACKLIST_PREFIX + accessToken, remainingMillis);
                redisTemplate.opsForValue().set(
                    BLACKLIST_PREFIX + accessToken,
                    "logout",
                    Duration.ofMillis(remainingMillis)
                );
            } else {
                log.warn("[AUTH] 토큰 이미 만료. remainingMillis={}", remainingMillis);
            }
        } catch (Exception e) {
            log.warn("[AUTH] 액세스 토큰 파싱 실패, 블랙리스트 미등록. reason={}", e.getMessage());
        }
    }

    @Override
    public void reportTheft(String token) {
        String userIdStr = redisTemplate.opsForValue().get(THEFT_REPORT_PREFIX + token);

        if (userIdStr == null) {
            throw new AuthException(AuthErrorCode.THEFT_REPORT_TOKEN_EXPIRED);
        }

        UUID userId = UUID.fromString(userIdStr);
        redisTemplate.opsForValue().set(
            FORCE_LOGOUT_PREFIX + userId,
            String.valueOf(Instant.now().toEpochMilli()),
            Duration.ofMillis(accessTokenValidity)
        );
        redisTemplate.delete("refresh:" + userId);
        redisTemplate.delete(THEFT_REPORT_PREFIX + token);
        log.warn("[AUTH] 본인 아님 신고 처리 완료. userId={}", userId);
    }

    private void sendSecurityAlertAsync(String email, String name, String ip, String userAgent, String token) {
        CompletableFuture.runAsync(() -> {
            try {
                String link = baseUrl + "/api/v1/auth/report-theft?token=" + token;
                String body = "<p>" + name + "님, 새로운 기기에서 로그인이 감지되었습니다.</p>"
                    + "<p>IP: " + ip + "</p>"
                    + "<p>기기: " + userAgent + "</p>"
                    + "<p>본인이 아닌 경우 아래 버튼을 클릭해 즉시 계정을 보호하세요. (10분 내 유효)</p>"
                    + "<a href='" + link + "' style='padding:10px 20px;background:#e53e3e;color:white;"
                    + "text-decoration:none;border-radius:4px;'>본인 아님 - 계정 보호</a>";
                mailService.send(email, "[보안 알림] 새로운 기기에서 로그인되었습니다.", body, true);
            } catch (Exception e) {
                log.error("[AUTH] 보안 알림 이메일 발송 실패. email={}", email, e);
            }
        });
    }
}
