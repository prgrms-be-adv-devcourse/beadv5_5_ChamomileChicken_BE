package jabaclass.payment.presentation.dto.request;

import java.util.UUID;

public record ConfirmDepositPaymentRequestDto(
	UUID depositPaymentsId
) {
}
