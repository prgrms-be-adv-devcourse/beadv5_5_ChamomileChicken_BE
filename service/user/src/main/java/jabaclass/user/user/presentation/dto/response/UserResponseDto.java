package jabaclass.user.user.presentation.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

import jabaclass.user.user.domain.model.User;
import jabaclass.user.user.domain.model.UserRole;

public record UserResponseDto(
	UUID userId,
	String name,
	String email,
	String phone,
	UserRole role,
	BigDecimal deposit
) {
	public static UserResponseDto from(User user) {
		return new UserResponseDto(
			user.getId(),
			user.getName(),
			user.getEmail(),
			user.getPhone(),
			user.getRole(),
			user.getDeposit()
		);
	}
}