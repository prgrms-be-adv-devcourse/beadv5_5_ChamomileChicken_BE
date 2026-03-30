package jabaclass.payment.presentation.controller;

import jabaclass.payment.application.usecase.PaymentUseCase;
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
	public ResponseEntity<PaymentResponseDto> preparePayment(@RequestBody PreparePaymentRequestDto request) {
		PaymentResponseDto response = paymentUseCase.create(request);

		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}

	@Override
	@PostMapping("/api/v1/payments/confirm")
	public ResponseEntity<PaymentResponseDto> confirmPayment(@RequestBody ConfirmPaymentRequestDto request) {
		PaymentResponseDto response = paymentUseCase.confirm(request);

		return ResponseEntity.ok(response);
	}

	@Override
	@PostMapping("/api/v1/refunds")
	public ResponseEntity<RefundPaymentResponseDto> refundPayment(@RequestBody RefundPaymentRequestDto request) {
		return ResponseEntity.ok(paymentUseCase.refund(request));
	}
}
