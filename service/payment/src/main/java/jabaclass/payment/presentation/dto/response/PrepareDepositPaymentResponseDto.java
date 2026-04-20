package jabaclass.payment.presentation.dto.response;

import jabaclass.payment.domain.model.DepositPayment;

import java.util.UUID;

public record PrepareDepositPaymentResponseDto(
	UUID depositPaymentsId
) {
	public static PrepareDepositPaymentResponseDto from(DepositPayment depositPayment) {
		return new PrepareDepositPaymentResponseDto(depositPayment.getId());
	}
}
