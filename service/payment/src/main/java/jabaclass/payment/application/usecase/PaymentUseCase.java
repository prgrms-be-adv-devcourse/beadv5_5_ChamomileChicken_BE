package jabaclass.payment.application.usecase;

import java.util.UUID;

import jabaclass.payment.presentation.dto.request.ConfirmPaymentRequestDto;
import jabaclass.payment.presentation.dto.request.InternalRefundRequestDto;
import jabaclass.payment.presentation.dto.request.PreparePaymentRequestDto;
import jabaclass.payment.presentation.dto.response.InternalRefundResponseDto;
import jabaclass.payment.presentation.dto.response.PaymentResponseDto;

public interface PaymentUseCase {

	PaymentResponseDto create(UUID userId, PreparePaymentRequestDto requestDto);

	PaymentResponseDto confirm(UUID userId, ConfirmPaymentRequestDto request);

InternalRefundResponseDto refundByOrder(InternalRefundRequestDto request);

}
