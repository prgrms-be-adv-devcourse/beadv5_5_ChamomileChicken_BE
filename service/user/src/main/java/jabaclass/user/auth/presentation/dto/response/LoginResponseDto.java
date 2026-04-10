package jabaclass.user.auth.presentation.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class LoginResponseDto {

    private String accessToken;

    @JsonIgnore
    private String refreshToken;

    private String role;
}
