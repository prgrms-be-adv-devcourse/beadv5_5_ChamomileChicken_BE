package jabaclass.product.infrastructure.acl.dto.response;

import java.util.UUID;

public record UserResponseDto(
	UUID userId,
	String name,
	String role
) {
	public static UserResponseDto from(UUID sellerId, String sellerName, String role) {
		return new UserResponseDto(
			sellerId,
			sellerName,
			role
		);
	}
}
