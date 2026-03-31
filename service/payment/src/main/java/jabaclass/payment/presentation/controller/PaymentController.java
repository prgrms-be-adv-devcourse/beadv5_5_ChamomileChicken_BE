package jabaclass.payment.presentation.controller;

import jabaclass.payment.application.usecase.PaymentUseCase;
import jabaclass.payment.common.dto.ApiResponseDto;
import jabaclass.payment.presentation.dto.request.ConfirmPaymentRequestDto;
import jabaclass.payment.presentation.dto.request.PreparePaymentRequestDto;
import jabaclass.payment.presentation.dto.request.RefundPaymentRequestDto;
import jabaclass.payment.presentation.dto.response.PaymentResponseDto;
import jabaclass.payment.presentation.dto.response.RefundPaymentResponseDto;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class PaymentController implements PaymentApi {

	private final PaymentUseCase paymentUseCase;

	@Override
	@PostMapping("/api/v1/payments/prepare")
	public ResponseEntity<ApiResponseDto<PaymentResponseDto>> preparePayment(@RequestBody PreparePaymentRequestDto request) {
		PaymentResponseDto response = paymentUseCase.create(request);

		return ResponseEntity.status(HttpStatus.CREATED)
			.body(ApiResponseDto.success(
				HttpStatus.CREATED,
				"결제 생성 성공",
				response
			));
	}

	@Override
	@PostMapping("/api/v1/payments/confirm")
	public ResponseEntity<ApiResponseDto<PaymentResponseDto>> confirmPayment(@RequestBody ConfirmPaymentRequestDto request) {
		PaymentResponseDto response = paymentUseCase.confirm(request);

		return ResponseEntity.ok(
			ApiResponseDto.success(
				HttpStatus.OK,
				"결제 성공",
				response
			)
		);
	}

	@Override
	@PostMapping("/api/v1/refunds")
	public ResponseEntity<ApiResponseDto<RefundPaymentResponseDto>> refundPayment(@RequestBody RefundPaymentRequestDto request) {
		return ResponseEntity.ok(
			ApiResponseDto.success(
				HttpStatus.OK,
				"환불 성공",
				paymentUseCase.refund(request)
			)
		);
	}
}
