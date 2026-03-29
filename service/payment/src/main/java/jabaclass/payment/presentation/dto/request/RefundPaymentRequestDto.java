package jabaclass.payment.presentation.dto.request;

import java.util.UUID;

public record RefundPaymentRequestDto(
	UUID orderId
) {
}
