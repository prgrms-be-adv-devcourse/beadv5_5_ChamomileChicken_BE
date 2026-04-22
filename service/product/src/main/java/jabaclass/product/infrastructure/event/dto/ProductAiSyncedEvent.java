package jabaclass.product.infrastructure.event.dto;

import java.math.BigDecimal;
import java.util.UUID;

import jabaclass.product.domain.model.Product;

public record ProductAiSyncedEvent(
	UUID eventId,
	UUID productId,
	String title,
	String description,
	BigDecimal price,
	String roadAddress,
	String status,
	Integer popularity
) {
	public static ProductAiSyncedEvent from(Product product) {
		return new ProductAiSyncedEvent(
			UUID.randomUUID(),
			product.getId(),
			product.getTitle(),
			product.getDescription(),
			product.getPrice(),
			product.getRoadAddress(),
			product.getStatus().name(),
			0
		);
	}
}
