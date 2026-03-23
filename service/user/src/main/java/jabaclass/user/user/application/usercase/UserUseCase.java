package jabaclass.user.user.application.usercase;

import java.util.UUID;

import jabaclass.user.user.presentation.dto.response.UserResponseDto;

public interface UserUseCase {

	UserResponseDto findMyInfo(UUID userId);
}
