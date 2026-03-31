package jabaclass.payment.presentation.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record RefundSettlementTargetItemResponseDto(
	UUID refundId,
	UUID paymentId,
	UUID orderId,
	UUID productId,
	String refundStatus,
	BigDecimal totalRefundAmount,
	LocalDateTime occurredAt,
	LocalDateTime updatedAt
) {
}
