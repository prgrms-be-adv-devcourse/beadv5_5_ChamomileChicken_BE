package jabaclass.settlement.infrastructure.kafka.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record SettlementRefundCompletedEvent(
	UUID eventId,
	UUID orderId,
	UUID paymentId,
	UUID refundId,
	UUID sellerId,
	UUID productId,
	BigDecimal settlementBaseAmount,
	LocalDateTime occurredAt
) {
}
