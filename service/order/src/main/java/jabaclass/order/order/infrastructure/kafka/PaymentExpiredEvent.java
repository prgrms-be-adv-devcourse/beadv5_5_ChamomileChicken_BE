package jabaclass.order.order.infrastructure.kafka;

import java.util.UUID;

public record PaymentExpiredEvent(
	UUID paymentId,
	UUID orderId
) {
}