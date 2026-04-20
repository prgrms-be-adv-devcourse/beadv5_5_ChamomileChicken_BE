package jabaclass.user.user.presentation.dto.request;

import jakarta.validation.constraints.NotBlank;

public record UpdateUserRequestDto(
	@NotBlank
	String name,

	@NotBlank
	String phone
) {
}