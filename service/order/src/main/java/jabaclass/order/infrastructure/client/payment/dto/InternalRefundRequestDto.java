package jabaclass.order.infrastructure.client.payment.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record InternalRefundRequestDto(
	UUID orderId,
	BigDecimal refundRate
) {
}