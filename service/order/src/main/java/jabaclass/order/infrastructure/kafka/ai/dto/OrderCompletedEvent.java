package jabaclass.order.infrastructure.kafka.ai.dto;

import java.util.UUID;

public record OrderCompletedEvent(
	UUID eventId,
	UUID orderId,
	UUID userId,
	UUID productId
) {
}
