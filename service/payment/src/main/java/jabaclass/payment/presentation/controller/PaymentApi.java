package jabaclass.payment.presentation.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jabaclass.payment.presentation.dto.request.PreparePaymentRequestDto;
import jabaclass.payment.presentation.dto.response.PaymentResponseDto;

import org.springframework.http.ResponseEntity;

@Tag(name = "Payment", description = "결제 API")
public interface PaymentApi {

	@Operation(
		summary = "결제 준비",
		description = """
			결제를 생성하고 준비 상태로 만듭니다.
			- payment 상태: READY
			- 총 금액 = paymentAmount + depositAmount
			"""
	)
	ResponseEntity<PaymentResponseDto> preparePayment(
		PreparePaymentRequestDto request
	);
}
