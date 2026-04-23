package jabaclass.product.infrastructure.event.dto;

import java.util.UUID;

public record ProductViewedEvent(
	UUID eventId,
	UUID userId,
	UUID productId
) {
	public static ProductViewedEvent of(UUID userId, UUID productId) {
		return new ProductViewedEvent(UUID.randomUUID(), userId, productId);
	}
}
