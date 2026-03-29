package jabaclass.product.infrastructure.acl.dto;

import java.util.UUID;

public record SellerResponseDto(
	UUID sellerId,
	String sellerName,
	String role
) {
	public static SellerResponseDto from(UUID sellerId, String sellerName, String role) {
		return new SellerResponseDto(
			sellerId,
			sellerName,
			role
		);
	}
}
