package jabaclass.product.infrastructure.kafka.order;

import java.util.UUID;

public record OrderRefundedEvent(
	UUID eventId,
	UUID orderId,
	UUID productUserId
) {
}