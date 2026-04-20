package jabaclass.order.infrastructure.kafka.payment.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record PaymentRefundCompletedEvent(
	UUID eventId,
	UUID paymentId,
	UUID orderId,
	BigDecimal depositRefundAmount
) {
}
