package jabaclass.payment.presentation.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record InternalRefundResponseDto(
	UUID refundId,
	UUID paymentId,
	UUID productId,
	BigDecimal depositRefundAmount,
	BigDecimal totalRefundAmount,
	LocalDateTime occurredAt
) {
}
