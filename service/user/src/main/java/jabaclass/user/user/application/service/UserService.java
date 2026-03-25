package jabaclass.user.user.application.service;

import java.math.BigDecimal;
import java.util.UUID;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jabaclass.user.common.error.BusinessException;
import jabaclass.user.mail.application.usecase.EmailVerificationUseCase;
import jabaclass.user.user.application.exception.UserErrorCode;
import jabaclass.user.user.application.usercase.UserUseCase;
import jabaclass.user.user.domain.model.User;
import jabaclass.user.user.domain.model.UserRole;
import jabaclass.user.user.domain.repository.UserRepository;
import jabaclass.user.user.presentation.dto.request.ChangeMyEmailRequestDto;
import jabaclass.user.user.presentation.dto.request.RegisterUserRequestDto;
import jabaclass.user.user.presentation.dto.request.UpdateUserRequestDto;
import jabaclass.user.user.presentation.dto.response.UserResponseDto;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService implements UserUseCase {

	private final UserRepository userRepository;
	private final EmailVerificationUseCase emailVerificationUseCase;


	@Override
	public void checkEmailDuplicate(String email) {
		if (userRepository.existsByEmail(email)) {
			throw new BusinessException(UserErrorCode.EMAIL_ALREADY_EXISTS);
		}
	}

	@Override
	@Transactional
	public void register(RegisterUserRequestDto request) {
		checkEmailDuplicate(request.email());
		emailVerificationUseCase.validateVerifiedToken(request.email(), request.verifiedToken());

		User user = User.builder()
			.name(request.name())
			.email(request.email())
			.password(request.password()) //Todo password 인코딩 처리
			.phone(request.phone())
			.role(UserRole.USER)
			.deposit(BigDecimal.ZERO)
			.build();

		userRepository.save(user);
	}

	@Override
	public UserResponseDto getMyInfo(UUID userId) {
		User user = getUser(userId);
		return UserResponseDto.from(user);
	}

	@Override
	@Transactional
	public void updateMyInfo(UUID userId, UpdateUserRequestDto request) {
		User user = getUser(userId);
		user.updateProfile(request.name(), request.phone());
	}

	@Override
	@Transactional
	public void changeEmail(UUID userId, ChangeMyEmailRequestDto request) {
		checkEmailDuplicate(request.newEmail());
		emailVerificationUseCase.validateVerifiedToken(request.newEmail(), request.verifiedToken());

		User user = getUser(userId);
		user.changeEmail(request.newEmail());
	}

	@Override
	@Transactional
	public void withdraw(UUID userId) {
		User user = getUser(userId);
		userRepository.delete(user);
	}

	private User getUser(UUID userId) {
		return userRepository.findById(userId)
			.orElseThrow(() -> new BusinessException(UserErrorCode.USER_NOT_FOUND));
	}
}