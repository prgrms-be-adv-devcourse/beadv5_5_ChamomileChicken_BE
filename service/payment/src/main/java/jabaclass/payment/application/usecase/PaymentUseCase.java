package jabaclass.payment.application.usecase;

import java.util.UUID;

import jabaclass.payment.presentation.dto.request.ConfirmPaymentRequestDto;
import jabaclass.payment.presentation.dto.request.PreparePaymentRequestDto;
import jabaclass.payment.presentation.dto.request.RefundPaymentRequestDto;
import jabaclass.payment.presentation.dto.response.PaymentResponseDto;
import jabaclass.payment.presentation.dto.response.RefundPaymentResponseDto;

public interface PaymentUseCase {

	PaymentResponseDto create(UUID userId, PreparePaymentRequestDto requestDto);

	PaymentResponseDto confirm(ConfirmPaymentRequestDto request);

	RefundPaymentResponseDto refund(RefundPaymentRequestDto request);

}
