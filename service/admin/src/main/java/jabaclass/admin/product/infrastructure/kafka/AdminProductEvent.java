package jabaclass.admin.product.infrastructure.kafka;

import java.util.UUID;

public record AdminProductEvent(String type, String productId) {

	public static AdminProductEvent forceDown(UUID productId) {
		return new AdminProductEvent("FORCE_DOWN", productId.toString());
	}
}
