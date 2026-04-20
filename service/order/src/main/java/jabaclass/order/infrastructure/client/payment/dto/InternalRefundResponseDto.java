package jabaclass.order.infrastructure.client.payment.dto;

import java.math.BigDecimal;

public record InternalRefundResponseDto(
	BigDecimal depositRefundAmount
) {
}