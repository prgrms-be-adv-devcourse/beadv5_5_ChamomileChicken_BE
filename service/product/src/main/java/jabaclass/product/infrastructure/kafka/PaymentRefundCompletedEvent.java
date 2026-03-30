package jabaclass.product.infrastructure.kafka;

import java.util.UUID;

public record PaymentRefundCompletedEvent(
	UUID orderId,
	UUID productUserId
) {
}
