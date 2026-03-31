package jabaclass.payment.presentation.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record PaymentSettlementTargetItemResponseDto(
	UUID paymentId,
	UUID orderId,
	UUID productId,
	String paymentStatus,
	BigDecimal totalPaymentAmount,
	LocalDateTime occurredAt,
	LocalDateTime updatedAt
) {
}
