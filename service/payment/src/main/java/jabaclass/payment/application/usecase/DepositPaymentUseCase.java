package jabaclass.payment.application.usecase;

import jabaclass.payment.presentation.dto.request.ConfirmDepositPaymentRequestDto;
import jabaclass.payment.presentation.dto.request.PrepareDepositPaymentRequestDto;
import jabaclass.payment.presentation.dto.response.ConfirmDepositPaymentResponseDto;
import jabaclass.payment.presentation.dto.response.PrepareDepositPaymentResponseDto;

public interface DepositPaymentUseCase {

	PrepareDepositPaymentResponseDto prepare(PrepareDepositPaymentRequestDto request);

	ConfirmDepositPaymentResponseDto confirm(ConfirmDepositPaymentRequestDto request);
}
