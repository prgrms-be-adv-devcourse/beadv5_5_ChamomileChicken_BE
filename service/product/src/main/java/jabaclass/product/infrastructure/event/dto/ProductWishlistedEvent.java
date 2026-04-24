package jabaclass.product.infrastructure.event.dto;

import java.util.UUID;

public record ProductWishlistedEvent(
	UUID eventId,
	UUID userId,
	UUID productId
) {
	public static ProductWishlistedEvent of(UUID userId, UUID productId) {
		return new ProductWishlistedEvent(UUID.randomUUID(), userId, productId);
	}
}
