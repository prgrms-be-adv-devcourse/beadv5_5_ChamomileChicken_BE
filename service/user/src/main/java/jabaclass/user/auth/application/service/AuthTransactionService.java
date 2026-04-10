package jabaclass.user.auth.application.service;

import java.util.UUID;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jabaclass.user.auth.application.exception.AuthErrorCode;
import jabaclass.user.auth.application.exception.AuthException;
import jabaclass.user.user.domain.model.User;
import jabaclass.user.user.domain.repository.UserRepository;

@Service
@RequiredArgsConstructor
@Transactional
public class AuthTransactionService {

	private final UserRepository userRepository;

	public void clearRefreshToken(UUID userId) {
		User user = userRepository.findById(userId)
			.orElseThrow(() -> new AuthException(AuthErrorCode.USER_NOT_FOUND));
		user.updateRefreshToken(null);
	}

	public User findUserWithLock(String email) {
		return userRepository.findByEmailWithLock(email)
			.orElseThrow(() -> new AuthException(AuthErrorCode.USER_NOT_FOUND));
	}
}
