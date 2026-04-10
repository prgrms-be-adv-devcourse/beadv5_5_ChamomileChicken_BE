package jabaclass.user.auth.application.usecase;

import jabaclass.user.auth.presentation.dto.request.LoginRequestDto;
import jabaclass.user.auth.presentation.dto.response.TokenResult;

public interface LoginUseCase {

    TokenResult login(LoginRequestDto request);
}
