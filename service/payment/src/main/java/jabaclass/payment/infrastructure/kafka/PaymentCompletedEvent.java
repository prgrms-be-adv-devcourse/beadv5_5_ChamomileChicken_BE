package jabaclass.payment.infrastructure.kafka;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record PaymentCompletedEvent(
	UUID eventId,
	UUID paymentId,
	UUID orderId,
	UUID productId,
	BigDecimal totalAmount,
	LocalDateTime occurredAt
) {
}
