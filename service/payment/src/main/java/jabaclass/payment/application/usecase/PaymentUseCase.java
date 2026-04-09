package jabaclass.payment.application.usecase;

import jabaclass.payment.presentation.dto.request.ConfirmPaymentRequestDto;
import jabaclass.payment.presentation.dto.request.PreparePaymentRequestDto;
import jabaclass.payment.presentation.dto.request.RefundPaymentRequestDto;
import jabaclass.payment.presentation.dto.response.PaymentResponseDto;
import jabaclass.payment.presentation.dto.response.RefundPaymentResponseDto;

public interface PaymentUseCase {

	PaymentResponseDto create(PreparePaymentRequestDto requestDto);

	PaymentResponseDto confirm(ConfirmPaymentRequestDto request);

	// RefundPaymentResponseDto refund(RefundPaymentRequestDto request);

}
