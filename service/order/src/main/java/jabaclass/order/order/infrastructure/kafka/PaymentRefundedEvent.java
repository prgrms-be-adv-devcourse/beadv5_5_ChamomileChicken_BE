package jabaclass.order.order.infrastructure.kafka;

import java.util.UUID;

public record PaymentRefundedEvent(
	UUID paymentId,
	UUID orderId
) {
}
