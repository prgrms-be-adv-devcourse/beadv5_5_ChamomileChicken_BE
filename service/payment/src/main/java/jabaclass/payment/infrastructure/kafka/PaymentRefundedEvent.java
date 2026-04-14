package jabaclass.payment.infrastructure.kafka;

import java.util.UUID;

public record PaymentRefundedEvent(
	UUID eventId,
	UUID paymentId,
	UUID orderId
) {
}