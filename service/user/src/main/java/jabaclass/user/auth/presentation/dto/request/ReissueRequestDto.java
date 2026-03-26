package jabaclass.user.auth.presentation.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
public class ReissueRequestDto {

    @NotBlank(message = "refresh token을 입력해주세요.")
    private String refreshToken;
}
