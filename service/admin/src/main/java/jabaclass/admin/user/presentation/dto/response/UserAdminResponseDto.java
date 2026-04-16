package jabaclass.admin.user.presentation.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

import jabaclass.admin.user.domain.model.User;
import jabaclass.admin.user.domain.model.UserRole;

public record UserAdminResponseDto(
	UUID id,
	String name,
	String email,
	String phone,
	UserRole role,
	LocalDateTime createdAt
) {
	public static UserAdminResponseDto from(User user) {
		return new UserAdminResponseDto(
			user.getId(),
			user.getName(),
			user.getEmail(),
			user.getPhone(),
			user.getRole(),
			user.getCreatedAt()
		);
	}
}
