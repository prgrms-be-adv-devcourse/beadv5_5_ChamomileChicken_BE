package jabaclass.order.infrastructure.kafka.payment.dto;

import java.util.UUID;

public record PaymentCompletedEvent(
	UUID orderId
) {
}
