package jabaclass.ai.infrastructure.kafka;

import java.math.BigDecimal;
import java.util.UUID;

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
}
