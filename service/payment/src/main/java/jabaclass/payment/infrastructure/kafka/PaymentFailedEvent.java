package jabaclass.payment.infrastructure.kafka;

import java.util.UUID;

public record PaymentFailedEvent(
	UUID orderId,
	UUID paymentId
) {}
