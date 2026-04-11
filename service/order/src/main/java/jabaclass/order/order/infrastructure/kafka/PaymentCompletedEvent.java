package jabaclass.order.order.infrastructure.kafka;

import java.util.UUID;

public record PaymentCompletedEvent(
	UUID paymentId,
	UUID orderId
) {}
