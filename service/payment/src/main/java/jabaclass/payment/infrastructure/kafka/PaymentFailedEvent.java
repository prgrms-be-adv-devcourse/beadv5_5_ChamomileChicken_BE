package jabaclass.payment.infrastructure.kafka;

import java.math.BigDecimal;
import java.util.UUID;

public record PaymentFailedEvent(
	UUID eventId,
	UUID paymentId,
	UUID orderId,
	BigDecimal depositAmount
) {
}