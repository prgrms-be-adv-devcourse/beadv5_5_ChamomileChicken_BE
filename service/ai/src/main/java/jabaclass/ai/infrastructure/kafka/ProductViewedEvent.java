package jabaclass.ai.infrastructure.kafka;

import java.util.UUID;

public record ProductViewedEvent(
	UUID eventId,
	UUID userId,
	UUID productId
) {
}
