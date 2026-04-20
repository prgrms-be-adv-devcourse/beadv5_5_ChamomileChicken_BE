package jabaclass.order.infrastructure.client.payment.dto;

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
