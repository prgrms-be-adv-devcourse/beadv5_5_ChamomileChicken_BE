package jabaclass.payment.infrastructure.kafka;

import java.util.UUID;

public record PaymentCompletedEvent(
	UUID orderId,
	UUID paymentId
) {}