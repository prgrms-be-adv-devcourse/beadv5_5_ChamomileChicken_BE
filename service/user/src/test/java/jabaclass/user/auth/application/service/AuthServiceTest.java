package jabaclass.user.auth.application.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Callable;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.jsonwebtoken.Claims;

import jabaclass.user.auth.application.exception.AuthErrorCode;
import jabaclass.user.auth.application.exception.AuthException;
import jabaclass.user.auth.domain.model.TokenBlacklist;
import jabaclass.user.auth.domain.repository.TokenBlacklistRepository;
import jabaclass.user.auth.infrastructure.jwt.JwtProvider;
import jabaclass.user.auth.infrastructure.jwt.TokenProvider;
import jabaclass.user.auth.presentation.dto.request.LoginRequestDto;
import jabaclass.user.auth.presentation.dto.response.TokenResult;
import jabaclass.user.auth.presentation.dto.response.TokenStatusResult;
import jabaclass.user.mail.application.service.MailService;
import jabaclass.user.user.domain.model.SocialType;
import jabaclass.user.user.domain.model.User;
import jabaclass.user.user.domain.model.UserRole;
import jabaclass.user.user.domain.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock UserRepository userRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock TokenProvider tokenProvider;
    @Mock JwtProvider jwtProvider;
    @Mock MailService mailService;
    @Mock TokenBlacklistRepository tokenBlacklistRepository;
    @Mock CircuitBreakerRegistry circuitBreakerRegistry;
    @Mock CircuitBreaker redisReadCb;
    @Mock CircuitBreaker redisWriteCb;
    @Mock StringRedisTemplate redisTemplate;
    @Mock ValueOperations<String, String> valueOps;
    @Mock LoginWriter loginWriter;

    @InjectMocks
    AuthService authService;

    @BeforeEach
    void setUp() throws Exception {
        when(circuitBreakerRegistry.circuitBreaker("redis-read", "redis-read")).thenReturn(redisReadCb);
        when(circuitBreakerRegistry.circuitBreaker("redis-write", "redis-write")).thenReturn(redisWriteCb);
        authService.init();

        ReflectionTestUtils.setField(authService, "refreshTokenValidity", 604800000L);
        ReflectionTestUtils.setField(authService, "accessTokenValidity", 300000L);
        ReflectionTestUtils.setField(authService, "baseUrl", "http://localhost:8080");

        // CB 기본 동작: pass-through (lenient — 특정 테스트에서 CB OPEN으로 override)
        lenient().doAnswer(inv -> inv.getArgument(0, Callable.class).call())
            .when(redisReadCb).executeCallable(any());
        lenient().doAnswer(inv -> {
            inv.getArgument(0, Runnable.class).run();
            return null;
        }).when(redisWriteCb).executeRunnable(any());

        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOps);
    }

    private User mockUser(UUID userId, String refreshToken) {
        User user = mock(User.class);
        lenient().when(user.getId()).thenReturn(userId);
        lenient().when(user.getEmail()).thenReturn("test@test.com");
        lenient().when(user.getName()).thenReturn("테스트유저");
        lenient().when(user.getRole()).thenReturn(UserRole.USER);
        lenient().when(user.getRefreshToken()).thenReturn(refreshToken);
        lenient().when(user.getLastLoginIp()).thenReturn(null);  // 최초 로그인 → 새 기기 알림 없음
        return user;
    }

    private LoginRequestDto mockLoginRequest(String email, String password) {
        LoginRequestDto req = mock(LoginRequestDto.class);
        when(req.getEmail()).thenReturn(email);
        lenient().when(req.getPassword()).thenReturn(password);
        return req;
    }

    // ────────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("login()")
    class LoginTest {

        @Test
        @DisplayName("정상 로그인 — DB와 Redis에 모두 저장")
        void success() {
            UUID userId = UUID.randomUUID();
            User user = mockUser(userId, null);
            when(user.getPassword()).thenReturn("encoded");
            when(userRepository.findByEmailAndSocialType("test@test.com", SocialType.SYSTEM))
                .thenReturn(Optional.of(user));
            when(passwordEncoder.matches("pw", "encoded")).thenReturn(true);
            when(tokenProvider.generateAccessToken(userId, UserRole.USER)).thenReturn("access");
            when(tokenProvider.generateRefreshToken(userId, UserRole.USER)).thenReturn("refresh");

            TokenResult result = authService.login(mockLoginRequest("test@test.com", "pw"), "127.0.0.1", "Agent");

            assertThat(result.getAccessToken()).isEqualTo("access");
            assertThat(result.getRefreshToken()).isEqualTo("refresh");
            verify(loginWriter).commitLoginWrites(userId, "refresh", "127.0.0.1", "Agent");        // DB 위임 확인
            verify(valueOps).set(eq("refresh:" + userId), eq("refresh"), any(Duration.class));     // Redis
        }

        @Test
        @DisplayName("존재하지 않는 이메일 — USER_NOT_FOUND")
        void userNotFound() {
            when(userRepository.findByEmailAndSocialType(anyString(), eq(SocialType.SYSTEM)))
                .thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.login(mockLoginRequest("none@test.com", "pw"), "127.0.0.1", "Agent"))
                .isInstanceOf(AuthException.class)
                .satisfies(e -> assertThat(((AuthException) e).getErrorCode()).isEqualTo(AuthErrorCode.USER_NOT_FOUND));
        }

        @Test
        @DisplayName("비밀번호 불일치 — USER_NOT_FOUND")
        void wrongPassword() {
            UUID userId = UUID.randomUUID();
            User user = mockUser(userId, null);
            when(user.getPassword()).thenReturn("encoded");
            when(userRepository.findByEmailAndSocialType("test@test.com", SocialType.SYSTEM))
                .thenReturn(Optional.of(user));
            when(passwordEncoder.matches("wrong", "encoded")).thenReturn(false);

            assertThatThrownBy(() -> authService.login(mockLoginRequest("test@test.com", "wrong"), "127.0.0.1", "Agent"))
                .isInstanceOf(AuthException.class)
                .satisfies(e -> assertThat(((AuthException) e).getErrorCode()).isEqualTo(AuthErrorCode.USER_NOT_FOUND));
        }

        @Test
        @DisplayName("Redis write CB OPEN — DB에는 저장되고 Redis는 스킵")
        void writeRedisOpen_dbSaved() {
            UUID userId = UUID.randomUUID();
            User user = mockUser(userId, null);
            when(user.getPassword()).thenReturn("encoded");
            when(userRepository.findByEmailAndSocialType("test@test.com", SocialType.SYSTEM))
                .thenReturn(Optional.of(user));
            when(passwordEncoder.matches("pw", "encoded")).thenReturn(true);
            when(tokenProvider.generateAccessToken(userId, UserRole.USER)).thenReturn("access");
            when(tokenProvider.generateRefreshToken(userId, UserRole.USER)).thenReturn("refresh");
            doThrow(mock(CallNotPermittedException.class)).when(redisWriteCb).executeRunnable(any());

            TokenResult result = authService.login(mockLoginRequest("test@test.com", "pw"), "127.0.0.1", "Agent");

            assertThat(result.getAccessToken()).isEqualTo("access");
            verify(loginWriter).commitLoginWrites(userId, "refresh", "127.0.0.1", "Agent"); // DB 위임 확인
            verify(valueOps, never()).set(anyString(), anyString(), any(Duration.class));    // Redis 스킵 확인
        }
    }

    // ────────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("reissue()")
    class ReissueTest {

        private static final String OLD_RT = "oldRefreshToken";
        private UUID userId;
        private Claims claims;

        @BeforeEach
        void setUp() {
            userId = UUID.randomUUID();
            claims = mock(Claims.class);
            when(jwtProvider.parseClaims(OLD_RT)).thenReturn(claims);
            when(jwtProvider.isRefreshToken(claims)).thenReturn(true);
            lenient().when(jwtProvider.getUserId(claims)).thenReturn(userId);
            lenient().when(jwtProvider.getRole(claims)).thenReturn("USER");
        }

        @Test
        @DisplayName("Redis hit — 정상 재발급")
        void success_redisHit() {
            User user = mockUser(userId, OLD_RT);
            when(valueOps.get("refresh:" + userId)).thenReturn(OLD_RT);
            when(userRepository.findById(userId)).thenReturn(Optional.of(user));
            when(tokenProvider.generateAccessToken(userId, UserRole.USER)).thenReturn("newAccess");
            when(tokenProvider.generateRefreshToken(userId, UserRole.USER)).thenReturn("newRefresh");

            TokenResult result = authService.reissue(OLD_RT);

            assertThat(result.getAccessToken()).isEqualTo("newAccess");
            assertThat(result.getRefreshToken()).isEqualTo("newRefresh");
            verify(user).updateRefreshToken("newRefresh");
        }

        @Test
        @DisplayName("Redis miss — DB fallback으로 재발급, findById 1회만 호출")
        void success_redisMiss_dbFallback() {
            User user = mockUser(userId, OLD_RT);
            when(valueOps.get("refresh:" + userId)).thenReturn(null);
            when(userRepository.findById(userId)).thenReturn(Optional.of(user));
            when(tokenProvider.generateAccessToken(userId, UserRole.USER)).thenReturn("newAccess");
            when(tokenProvider.generateRefreshToken(userId, UserRole.USER)).thenReturn("newRefresh");

            TokenResult result = authService.reissue(OLD_RT);

            assertThat(result.getAccessToken()).isEqualTo("newAccess");
            verify(userRepository, times(1)).findById(userId);  // lazy — DB 1회만 조회
        }

        @Test
        @DisplayName("Redis read CB OPEN — DB fallback으로 재발급")
        void cbOpen_dbFallback() throws Exception {
            User user = mockUser(userId, OLD_RT);
            doThrow(mock(CallNotPermittedException.class)).when(redisReadCb).executeCallable(any());
            when(userRepository.findById(userId)).thenReturn(Optional.of(user));
            when(tokenProvider.generateAccessToken(userId, UserRole.USER)).thenReturn("newAccess");
            when(tokenProvider.generateRefreshToken(userId, UserRole.USER)).thenReturn("newRefresh");

            TokenResult result = authService.reissue(OLD_RT);

            assertThat(result.getAccessToken()).isEqualTo("newAccess");
        }

        @Test
        @DisplayName("DB의 refreshToken이 null — ALREADY_LOGGED_OUT")
        void alreadyLoggedOut() {
            User user = mockUser(userId, null);  // refreshToken = null
            when(valueOps.get("refresh:" + userId)).thenReturn(null);
            when(userRepository.findById(userId)).thenReturn(Optional.of(user));

            assertThatThrownBy(() -> authService.reissue(OLD_RT))
                .isInstanceOf(AuthException.class)
                .satisfies(e -> assertThat(((AuthException) e).getErrorCode())
                    .isEqualTo(AuthErrorCode.ALREADY_LOGGED_OUT));
        }

        @Test
        @DisplayName("RTR 감지 — SUSPECTED_TOKEN_THEFT + DB에 forceLogout 저장")
        void rtrDetected() {
            User user = mockUser(userId, "storedToken");  // stored != provided
            when(valueOps.get("refresh:" + userId)).thenReturn("storedToken");
            when(userRepository.findById(userId)).thenReturn(Optional.of(user));

            assertThatThrownBy(() -> authService.reissue(OLD_RT))
                .isInstanceOf(AuthException.class)
                .satisfies(e -> assertThat(((AuthException) e).getErrorCode())
                    .isEqualTo(AuthErrorCode.SUSPECTED_TOKEN_THEFT));

            verify(user).forceLogout();
            verify(user).updateRefreshToken(null);
        }

        @Test
        @DisplayName("Refresh Token이 아닌 토큰 — INVALID_REFRESH_TOKEN")
        void invalidTokenType() {
            when(jwtProvider.isRefreshToken(claims)).thenReturn(false);

            assertThatThrownBy(() -> authService.reissue(OLD_RT))
                .isInstanceOf(AuthException.class)
                .satisfies(e -> assertThat(((AuthException) e).getErrorCode())
                    .isEqualTo(AuthErrorCode.INVALID_REFRESH_TOKEN));
        }
    }

    // ────────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("logout()")
    class LogoutTest {

        @Test
        @DisplayName("정상 로그아웃 — DB blacklist 저장 + Redis write")
        void success() {
            UUID userId = UUID.randomUUID();
            User user = mockUser(userId, "refresh");
            when(userRepository.findById(userId)).thenReturn(Optional.of(user));

            long future = System.currentTimeMillis() + 300_000L;
            Claims claims = mock(Claims.class);
            when(jwtProvider.parseClaims("access")).thenReturn(claims);
            when(claims.getExpiration()).thenReturn(new Date(future));

            authService.logout(userId, "access");

            verify(user).updateRefreshToken(null);
            verify(tokenBlacklistRepository).save(any(TokenBlacklist.class));
            verify(valueOps).set(startsWith("blacklist:"), eq("logout"), any(Duration.class));
        }

        @Test
        @DisplayName("이미 만료된 accessToken — blacklist 미등록")
        void expiredToken_noBlacklist() {
            UUID userId = UUID.randomUUID();
            User user = mockUser(userId, "refresh");
            when(userRepository.findById(userId)).thenReturn(Optional.of(user));

            long past = System.currentTimeMillis() - 1_000L;
            Claims claims = mock(Claims.class);
            when(jwtProvider.parseClaims("expired")).thenReturn(claims);
            when(claims.getExpiration()).thenReturn(new Date(past));

            authService.logout(userId, "expired");

            verify(tokenBlacklistRepository, never()).save(any());
        }

        @Test
        @DisplayName("Redis write CB OPEN — DB blacklist 저장, Redis 스킵")
        void writeRedisOpen_dbBlacklistSaved() {
            UUID userId = UUID.randomUUID();
            User user = mockUser(userId, "refresh");
            when(userRepository.findById(userId)).thenReturn(Optional.of(user));

            long future = System.currentTimeMillis() + 300_000L;
            Claims claims = mock(Claims.class);
            when(jwtProvider.parseClaims("access")).thenReturn(claims);
            when(claims.getExpiration()).thenReturn(new Date(future));
            doThrow(mock(CallNotPermittedException.class)).when(redisWriteCb).executeRunnable(any());

            authService.logout(userId, "access");

            verify(tokenBlacklistRepository).save(any(TokenBlacklist.class));               // DB 저장 확인
            verify(valueOps, never()).set(anyString(), anyString(), any(Duration.class));   // Redis 스킵 확인
        }
    }

    // ────────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("reportTheft()")
    class ReportTheftTest {

        @Test
        @DisplayName("정상 신고 — forceLogout + Redis 정리")
        void success() throws Exception {
            UUID userId = UUID.randomUUID();
            User user = mockUser(userId, "refresh");
            String token = "theftToken";

            when(valueOps.get("theft_report:" + token)).thenReturn(userId.toString());
            when(userRepository.findById(userId)).thenReturn(Optional.of(user));

            authService.reportTheft(token);

            verify(user).forceLogout();
            verify(user).updateRefreshToken(null);
            verify(redisTemplate).delete("theft_report:" + token);
        }

        @Test
        @DisplayName("Redis에 토큰 없음 — THEFT_REPORT_TOKEN_EXPIRED")
        void tokenNotFound() throws Exception {
            when(valueOps.get(startsWith("theft_report:"))).thenReturn(null);

            assertThatThrownBy(() -> authService.reportTheft("invalid"))
                .isInstanceOf(AuthException.class)
                .satisfies(e -> assertThat(((AuthException) e).getErrorCode())
                    .isEqualTo(AuthErrorCode.THEFT_REPORT_TOKEN_EXPIRED));
        }

        @Test
        @DisplayName("Redis read CB OPEN — THEFT_REPORT_TOKEN_EXPIRED")
        void cbOpen_throwsExpired() throws Exception {
            doThrow(mock(CallNotPermittedException.class)).when(redisReadCb).executeCallable(any());

            assertThatThrownBy(() -> authService.reportTheft("token"))
                .isInstanceOf(AuthException.class)
                .satisfies(e -> assertThat(((AuthException) e).getErrorCode())
                    .isEqualTo(AuthErrorCode.THEFT_REPORT_TOKEN_EXPIRED));
        }
    }

    // ────────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("checkTokenStatus()")
    class CheckTokenStatusTest {

        @Test
        @DisplayName("유효한 토큰 — valid")
        void valid() {
            UUID userId = UUID.randomUUID();
            when(tokenBlacklistRepository.existsByTokenHashAndExpiresAtAfter(any(), any())).thenReturn(false);
            User user = mockUser(userId, null);
            when(user.getForceLogoutAt()).thenReturn(null);
            when(userRepository.findById(userId)).thenReturn(Optional.of(user));

            TokenStatusResult result = authService.checkTokenStatus("token", userId, System.currentTimeMillis());

            assertThat(result.tokenValid()).isTrue();
        }

        @Test
        @DisplayName("블랙리스트 토큰 — blacklisted")
        void blacklisted() {
            UUID userId = UUID.randomUUID();
            when(tokenBlacklistRepository.existsByTokenHashAndExpiresAtAfter(any(), any())).thenReturn(true);

            TokenStatusResult result = authService.checkTokenStatus("token", userId, System.currentTimeMillis());

            assertThat(result.tokenValid()).isFalse();
            assertThat(result.reason()).isEqualTo("BLACKLISTED");
        }

        @Test
        @DisplayName("forceLogout 이전 발급 토큰 — forceLogout")
        void forceLogout() {
            UUID userId = UUID.randomUUID();
            when(tokenBlacklistRepository.existsByTokenHashAndExpiresAtAfter(any(), any())).thenReturn(false);

            LocalDateTime forceLogoutAt = LocalDateTime.now();
            User user = mockUser(userId, null);
            when(user.getForceLogoutAt()).thenReturn(forceLogoutAt);
            when(userRepository.findById(userId)).thenReturn(Optional.of(user));

            long issuedBefore = forceLogoutAt.minusSeconds(10)
                .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();

            TokenStatusResult result = authService.checkTokenStatus("token", userId, issuedBefore);

            assertThat(result.tokenValid()).isFalse();
            assertThat(result.reason()).isEqualTo("FORCE_LOGOUT");
        }
    }

    // ────────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("issueOAuth2Tokens()")
    class IssueOAuth2TokensTest {

        @Test
        @DisplayName("OAuth2 토큰 발급 — DB와 Redis에 모두 저장")
        void success() {
            UUID userId = UUID.randomUUID();
            User user = mockUser(userId, null);
            when(userRepository.findById(userId)).thenReturn(Optional.of(user));
            when(tokenProvider.generateAccessToken(userId, UserRole.USER)).thenReturn("access");
            when(tokenProvider.generateRefreshToken(userId, UserRole.USER)).thenReturn("refresh");

            TokenResult result = authService.issueOAuth2Tokens(userId, UserRole.USER, "127.0.0.1", "test-agent");

            assertThat(result.getAccessToken()).isEqualTo("access");
            assertThat(result.getRefreshToken()).isEqualTo("refresh");
            verify(user).updateRefreshToken("refresh");                                         // DB-first
            verify(valueOps).set(eq("refresh:" + userId), eq("refresh"), any(Duration.class)); // Redis
        }
    }
}
