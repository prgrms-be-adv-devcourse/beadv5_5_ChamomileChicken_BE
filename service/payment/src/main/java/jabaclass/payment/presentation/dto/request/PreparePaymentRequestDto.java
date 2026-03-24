package jabaclass.payment.presentation.dto.request;

import jabaclass.payment.domain.model.PaymentMethod;

import java.math.BigDecimal;
import java.util.UUID;

public record PreparePaymentRequestDto(
	UUID userId,
	UUID sellerId,
	UUID productId,
	UUID orderId,
	PaymentMethod paymentMethod,
	BigDecimal paymentAmount,
	BigDecimal depositAmount
) {
}
