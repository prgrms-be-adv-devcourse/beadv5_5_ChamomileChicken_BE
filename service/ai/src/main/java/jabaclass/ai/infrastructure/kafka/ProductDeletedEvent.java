package jabaclass.ai.infrastructure.kafka;

import java.util.UUID;

public record ProductDeletedEvent(
	UUID eventId,
	UUID productId
) {
}
