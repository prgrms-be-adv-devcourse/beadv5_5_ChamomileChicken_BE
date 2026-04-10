package jabaclass.user.auth.application.usecase;

import jabaclass.user.auth.presentation.dto.response.TokenResult;

public interface ReissueUseCase {

    TokenResult reissue(String refreshToken);
}
