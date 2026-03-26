package jabaclass.user.auth.application.usecase;

import jabaclass.user.auth.presentation.dto.request.ReissueRequestDto;
import jabaclass.user.auth.presentation.dto.response.LoginResponseDto;

public interface ReissueUseCase {

    LoginResponseDto reissue(ReissueRequestDto requestDto);
}
