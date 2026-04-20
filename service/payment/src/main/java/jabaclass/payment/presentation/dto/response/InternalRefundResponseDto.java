package jabaclass.payment.presentation.dto.response;

import java.math.BigDecimal;

public record InternalRefundResponseDto(
	BigDecimal depositRefundAmount
) {
}