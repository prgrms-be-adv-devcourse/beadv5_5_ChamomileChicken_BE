package jabaclass.payment.presentation.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jabaclass.payment.presentation.dto.request.ConfirmPaymentRequestDto;
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

	@Operation(
		summary = "결제 승인",
		description = """
            PG 결제 승인 요청을 처리합니다.
            - 결제 금액 검증
            - PG(Toss) 승인 API 호출
            - payment 상태: DONE 또는 FAILED
            - 주문 상태 업데이트
            """
	)
	ResponseEntity<PaymentResponseDto> confirmPayment(
		ConfirmPaymentRequestDto request
	);
}
