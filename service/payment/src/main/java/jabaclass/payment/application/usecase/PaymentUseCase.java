package jabaclass.payment.application.usecase;

import java.util.UUID;

import jabaclass.payment.presentation.dto.request.ConfirmPaymentRequestDto;
import jabaclass.payment.presentation.dto.request.PreparePaymentRequestDto;
import jabaclass.payment.presentation.dto.request.RefundPaymentRequestDto;
import jabaclass.payment.presentation.dto.response.PaymentResponseDto;

public interface PaymentUseCase {

	PaymentResponseDto create(UUID userId,PreparePaymentRequestDto requestDto);

	PaymentResponseDto confirm(UUID userId,ConfirmPaymentRequestDto request);

	void refund(UUID userId,RefundPaymentRequestDto request);

}
