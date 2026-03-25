package jabaclass.user.auth.presentation.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record SendVerificationCodeRequestDto(
	@NotBlank
	@Email
	String email
) {
}