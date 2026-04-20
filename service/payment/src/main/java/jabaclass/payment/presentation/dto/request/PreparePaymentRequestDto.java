package jabaclass.payment.presentation.dto.request;

import java.math.BigDecimal;
import java.util.UUID;

import jabaclass.payment.domain.model.PaymentMethod;

public record PreparePaymentRequestDto(
	UUID productId,
	UUID orderId,
	UUID userId,
	PaymentMethod paymentMethod,
	BigDecimal paymentAmount,
	BigDecimal depositAmount
) {
}
