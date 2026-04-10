package jabaclass.user.auth.application.service;

import java.time.Duration;
import java.util.UUID;

import io.jsonwebtoken.Claims;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jabaclass.user.user.domain.model.UserRole;
import jabaclass.auth.jwt.JwtProvider;
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

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService implements LoginUseCase, LogoutUseCase, ReissueUseCase {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenProvider tokenProvider;
    private final JwtProvider jwtProvider;

    private static final String BLACKLIST_PREFIX = "blacklist:";
    private final StringRedisTemplate redisTemplate;

    @Value("${jwt.refresh-token-validity}")
    private long refreshTokenValidity;

    @Override
    @Transactional
    public TokenResult login(LoginRequestDto request) {

        User user = userRepository.findByEmailWithLock(request.getEmail())
                .orElseThrow(() -> new AuthException(AuthErrorCode.USER_NOT_FOUND));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new AuthException(AuthErrorCode.INVALID_PASSWORD);
        }

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
            throw new AuthException(AuthErrorCode.REFRESH_TOKEN_MISMATCH);
        }

        String newAccessToken = tokenProvider.generateAccessToken(userId, UserRole.valueOf(role));
        String newRefreshToken = tokenProvider.generateRefreshToken(userId, UserRole.valueOf(role));

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
}
