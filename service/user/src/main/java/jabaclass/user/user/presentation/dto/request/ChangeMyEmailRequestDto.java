package jabaclass.user.user.presentation.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record ChangeMyEmailRequestDto(
	@NotBlank
	@Email
	String newEmail,

	@NotBlank
	String verifiedToken
) {
}