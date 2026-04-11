package jabaclass.payment.presentation.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jabaclass.payment.common.dto.ApiResponseDto;
import jabaclass.payment.presentation.dto.request.ConfirmPaymentRequestDto;
import jabaclass.payment.presentation.dto.request.PreparePaymentRequestDto;
import jabaclass.payment.presentation.dto.request.RefundPaymentRequestDto;
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
	ResponseEntity<ApiResponseDto<PaymentResponseDto>> preparePayment(
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
	ResponseEntity<ApiResponseDto<PaymentResponseDto>> confirmPayment(
		ConfirmPaymentRequestDto request
	);

	@Operation(
		summary = "환불 요청",
		description = """
			orderId로 환불을 요청합니다.
			- 예치금 100% 결제면 Toss 환불 호출을 생략합니다.
			- 예치금 환불이 필요하면 user 서비스에 동기 호출합니다.
			- 완료 후 payment.refund.completed 이벤트를 발행합니다.
			"""
	)
	ResponseEntity<ApiResponseDto<Void>> refundPayment(
		RefundPaymentRequestDto request
	);
}
