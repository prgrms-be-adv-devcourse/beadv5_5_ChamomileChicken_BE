package jabaclass.user.user.application.usercase;

import java.util.UUID;

import jabaclass.user.user.presentation.dto.request.ChangeMyEmailRequestDto;
import jabaclass.user.user.presentation.dto.request.RegisterUserRequestDto;
import jabaclass.user.user.presentation.dto.request.UpdateUserRequestDto;
import jabaclass.user.user.presentation.dto.response.UserResponseDto;

public interface UserUseCase {

	void checkEmailDuplicate(String email);

	void register(RegisterUserRequestDto request);

	UserResponseDto getMyInfo(UUID userId);

	void updateMyInfo(UUID userId, UpdateUserRequestDto request);

	void changeEmail(UUID userId, ChangeMyEmailRequestDto request);

	void withdraw(UUID userId);
}