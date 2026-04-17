package jabaclass.payment.infrastructure.kafka;

import java.math.BigDecimal;
import java.util.UUID;

public record PaymentExpiredEvent(
	UUID eventId,
	UUID paymentId,
	UUID orderId,
	BigDecimal depositAmount
) {
}
