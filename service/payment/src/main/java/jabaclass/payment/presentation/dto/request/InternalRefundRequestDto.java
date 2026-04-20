package jabaclass.payment.presentation.dto.request;

import java.math.BigDecimal;
import java.util.UUID;

public record InternalRefundRequestDto(
	UUID orderId,
	BigDecimal refundRate
) {
}