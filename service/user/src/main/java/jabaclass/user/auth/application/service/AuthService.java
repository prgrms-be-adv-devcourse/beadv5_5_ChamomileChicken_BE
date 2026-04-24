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
                Duration.ofMillis(refreshTokenValidity)
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
}
