package jabaclass.product.infrastructure.kafka.payment;

import java.util.UUID;

public record PaymentRefundCompletedEvent(
	UUID eventId,
	UUID productUserId
) {
}
