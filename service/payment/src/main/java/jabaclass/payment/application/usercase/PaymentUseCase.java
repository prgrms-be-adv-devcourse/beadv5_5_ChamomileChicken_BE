package jabaclass.payment.application.usercase;

import jabaclass.payment.presentation.dto.request.PreparePaymentRequestDto;
import jabaclass.payment.presentation.dto.response.PaymentResponseDto;

public interface PaymentUseCase {

	PaymentResponseDto create(PreparePaymentRequestDto requestDto);

}
