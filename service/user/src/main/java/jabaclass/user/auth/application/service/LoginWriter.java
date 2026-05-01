package jabaclass.user.auth.application.service;

import java.util.UUID;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import jabaclass.user.auth.application.exception.AuthErrorCode;
import jabaclass.user.auth.application.exception.AuthException;
import jabaclass.user.user.domain.model.User;
import jabaclass.user.user.domain.repository.UserRepository;

@Component
@RequiredArgsConstructor
@Slf4j
public class LoginWriter {

    private final UserRepository userRepository;

    @Transactional
    public void commitLoginWrites(UUID userId, String refreshToken, String clientIp, String userAgent) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new AuthException(AuthErrorCode.USER_NOT_FOUND));
        user.updateRefreshToken(refreshToken);
        user.updateLastLogin(clientIp, userAgent);
    }
}
