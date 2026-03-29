package jabaclass.payment.application.usecase;

import jabaclass.payment.presentation.dto.request.ConfirmPaymentRequestDto;
import jabaclass.payment.presentation.dto.request.PreparePaymentRequestDto;
import jabaclass.payment.presentation.dto.response.PaymentResponseDto;

public interface PaymentUseCase {

	PaymentResponseDto create(PreparePaymentRequestDto requestDto);

	PaymentResponseDto confirm(ConfirmPaymentRequestDto request);

}
