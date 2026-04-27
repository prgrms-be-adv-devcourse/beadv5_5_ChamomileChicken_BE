package jabaclass.order.infrastructure.client.payment.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record PaymentRefundInfoResponseDto(
	UUID orderId,
	BigDecimal refundRate,
	BigDecimal paymentRefundAmount,
	BigDecimal depositRefundAmount,
	BigDecimal totalRefundAmount,
	LocalDateTime requestedAt,
	LocalDateTime processedAt
) {
}
