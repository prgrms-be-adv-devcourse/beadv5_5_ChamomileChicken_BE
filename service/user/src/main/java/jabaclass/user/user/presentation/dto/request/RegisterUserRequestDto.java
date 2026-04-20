package jabaclass.user.user.presentation.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record RegisterUserRequestDto(
	@NotBlank
	String name,

	@NotBlank
	@Email
	String email,

	@NotBlank
	String password,

	@NotBlank
	String phone,

	@NotBlank
	String verifiedToken
) {
}