package jabaclass.payment.infrastructure.kafka;

import java.util.UUID;

public record PaymentCompletedEvent(
	UUID eventId,
	UUID paymentId,
	UUID orderId
) {
}
