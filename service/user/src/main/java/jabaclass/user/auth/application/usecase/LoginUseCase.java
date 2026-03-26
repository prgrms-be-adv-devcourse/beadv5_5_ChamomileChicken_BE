package jabaclass.user.auth.application.usecase;

import jabaclass.user.auth.presentation.dto.request.LoginRequestDto;
import jabaclass.user.auth.presentation.dto.response.LoginResponseDto;

public interface LoginUseCase {

    LoginResponseDto login(LoginRequestDto request);
}
