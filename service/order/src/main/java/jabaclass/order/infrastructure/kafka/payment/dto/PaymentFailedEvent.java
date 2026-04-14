package jabaclass.order.infrastructure.kafka.payment.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record PaymentFailedEvent(
	UUID orderId,
	BigDecimal depositAmount
) {
}