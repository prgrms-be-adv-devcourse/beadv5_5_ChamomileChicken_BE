package jabaclass.payment.presentation.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jabaclass.payment.application.usecase.PaymentUseCase;
import jabaclass.payment.common.dto.ApiResponseDto;
import jabaclass.payment.presentation.dto.request.ConfirmPaymentRequestDto;
import jabaclass.payment.presentation.dto.request.PreparePaymentRequestDto;
import jabaclass.payment.presentation.dto.request.RefundPaymentRequestDto;
import jabaclass.payment.presentation.dto.response.PaymentResponseDto;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/payments")
public class PaymentController implements PaymentApi {

	private final PaymentUseCase paymentUseCase;

	@Override
	@PostMapping("/prepare")
	public ResponseEntity<ApiResponseDto<PaymentResponseDto>> preparePayment(
		@RequestBody PreparePaymentRequestDto request) {
		//UUID userId = SecurityUtil.getCurrentUserId();

		PaymentResponseDto response = paymentUseCase.create(request);

		return ResponseEntity.status(HttpStatus.CREATED)
			.body(ApiResponseDto.success(
				HttpStatus.CREATED,
				"결제 생성 성공",
				response
			));
	}

	@Override
	@PostMapping("/confirm")
	public ResponseEntity<ApiResponseDto<PaymentResponseDto>> confirmPayment(
		@RequestBody ConfirmPaymentRequestDto request) {
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
	@PostMapping("/refunds")
	public ResponseEntity<ApiResponseDto<Void>> refundPayment(
		@RequestBody RefundPaymentRequestDto request) {

		paymentUseCase.refund(request);

		return ResponseEntity.status(HttpStatus.ACCEPTED).body(
			ApiResponseDto.success(
				HttpStatus.ACCEPTED,
				"환불 요청이 접수되었습니다.",
				null
			)
		);
	}
}
