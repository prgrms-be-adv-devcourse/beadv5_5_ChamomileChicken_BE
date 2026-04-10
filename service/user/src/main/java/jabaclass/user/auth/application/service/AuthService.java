package jabaclass.user.auth.application.service;

import io.jsonwebtoken.Claims;
import jabaclass.auth.jwt.JwtProvider;
import jabaclass.user.auth.application.exception.AuthErrorCode;
import jabaclass.user.auth.application.exception.AuthException;
import jabaclass.user.auth.application.usecase.LoginUseCase;
import jabaclass.user.auth.application.usecase.LogoutUseCase;
import jabaclass.user.auth.application.usecase.ReissueUseCase;
import jabaclass.user.auth.infrastructure.jwt.TokenProvider;
import jabaclass.user.auth.presentation.dto.request.LoginRequestDto;
import jabaclass.user.auth.presentation.dto.response.LoginResponseDto;
import jabaclass.user.user.domain.model.User;
import jabaclass.user.user.domain.repository.UserRepository;

import java.time.Duration;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService implements LoginUseCase, LogoutUseCase, ReissueUseCase {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenProvider tokenProvider;
    private final JwtProvider jwtProvider;
    private final AuthTransactionService authTransactionService;

    private static final String BLACKLIST_PREFIX = "blacklist:";
    private final StringRedisTemplate redisTemplate;

    @Override
    @Transactional
    public LoginResponseDto login(LoginRequestDto request) {

        User user = userRepository.findByEmailWithLock(request.getEmail())
                .orElseThrow(() -> new AuthException(AuthErrorCode.USER_NOT_FOUND));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new AuthException(AuthErrorCode.INVALID_PASSWORD);
        }

        String accessToken = tokenProvider.generateAccessToken(user.getId());
        String refreshToken = tokenProvider.generateRefreshToken(user.getId());

        user.updateRefreshToken(refreshToken);

        return new LoginResponseDto(accessToken, refreshToken, user.getRole().name());
    }

    @Override
    @Transactional
    public LoginResponseDto reissue(String refreshToken) {
        Claims claims = jwtProvider.parseClaims(refreshToken);

        if (!jwtProvider.isRefreshToken(claims)) {
            throw new AuthException(AuthErrorCode.INVALID_REFRESH_TOKEN);
        }

        UUID userId = jwtProvider.getUserId(claims);

        User user = userRepository.findById(userId)
            .orElseThrow(() -> new AuthException(AuthErrorCode.USER_NOT_FOUND));

        if (user.getRefreshToken() == null) {
            throw new AuthException(AuthErrorCode.ALREADY_LOGGED_OUT);
        }

        if (!user.getRefreshToken().equals(refreshToken)) {
            throw new AuthException(AuthErrorCode.REFRESH_TOKEN_MISMATCH);
        }

        String newAccessToken = tokenProvider.generateAccessToken(userId);
        String newRefreshToken = tokenProvider.generateRefreshToken(userId);

        user.updateRefreshToken(newRefreshToken);

        return new LoginResponseDto(newAccessToken, newRefreshToken, user.getRole().name());
    }

    @Override
    @Transactional
    public void logout(UUID userId, String accessToken) {
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

        authTransactionService.clearRefreshToken(userId);
    }
}
