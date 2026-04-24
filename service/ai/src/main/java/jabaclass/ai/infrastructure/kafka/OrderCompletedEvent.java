package jabaclass.ai.infrastructure.kafka;

import java.util.UUID;

public record OrderCompletedEvent(
	UUID eventId,
	UUID orderId,
	UUID userId,
	UUID productId
) {
}
