package jabaclass.payment.presentation.dto.request;

import java.math.BigDecimal;
import java.util.UUID;

public record PrepareDepositPaymentRequestDto(
	UUID userId,
	BigDecimal amount,
	String paymentMethod
) {
}
