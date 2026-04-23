package jabaclass.product.infrastructure.event.dto;

import java.util.UUID;

public record ProductDeletedEvent(
	UUID eventId,
	UUID productId
) {
	public static ProductDeletedEvent of(UUID productId) {
		return new ProductDeletedEvent(UUID.randomUUID(), productId);
	}
}
