package jabaclass.order.infrastructure.kafka.settlement.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record SettlementPaymentCompletedEvent(
	UUID eventId,
	UUID orderId,
	UUID paymentId,
	UUID sellerId,
	UUID productId,
	BigDecimal settlementBaseAmount,
	LocalDateTime occurredAt
) {
}
