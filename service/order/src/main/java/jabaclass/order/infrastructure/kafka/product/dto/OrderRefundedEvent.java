package jabaclass.order.infrastructure.kafka.product.dto;

import java.util.UUID;

public record OrderRefundedEvent(
	UUID eventId,
	UUID orderId,
	UUID productUserId
) {
}