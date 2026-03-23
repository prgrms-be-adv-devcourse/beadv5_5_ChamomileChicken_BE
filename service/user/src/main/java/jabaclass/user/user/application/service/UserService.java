package jabaclass.user.user.application.service;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jabaclass.user.common.error.BusinessException;
import jabaclass.user.user.application.exception.UserErrorCode;
import jabaclass.user.user.application.usercase.UserUseCase;
import jabaclass.user.user.domain.model.User;
import jabaclass.user.user.domain.repository.UserRepository;
import jabaclass.user.user.presentation.dto.response.UserResponseDto;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService implements UserUseCase {

	private final UserRepository userRepository;

	@Override
	public UserResponseDto findMyInfo(UUID userId) {
		User user = userRepository.findById(userId)
			.orElseThrow(() -> new BusinessException(UserErrorCode.USER_NOT_FOUND)); //Todo 커스텀 예외 추가

		return UserResponseDto.from(user);
	}
}