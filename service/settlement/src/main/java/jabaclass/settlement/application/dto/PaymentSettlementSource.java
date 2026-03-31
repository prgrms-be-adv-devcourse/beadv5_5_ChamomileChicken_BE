package jabaclass.settlement.application.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record PaymentSettlementSource(
	UUID paymentId,
	UUID orderId,
	UUID productId,
	String paymentStatus,
	BigDecimal totalPaymentAmount,
	LocalDateTime occurredAt,
	LocalDateTime updatedAt
) {
}