package jabaclass.payment.presentation.dto.request;

import java.util.UUID;

public record ConfirmPaymentRequestDto(
	UUID paymentId,
	String paymentKey,
	int amount
) {
}
