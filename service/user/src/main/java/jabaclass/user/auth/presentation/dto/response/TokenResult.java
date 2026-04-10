package jabaclass.user.auth.presentation.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class TokenResult {
	private String accessToken;
	private String refreshToken;
	private String role;
}
