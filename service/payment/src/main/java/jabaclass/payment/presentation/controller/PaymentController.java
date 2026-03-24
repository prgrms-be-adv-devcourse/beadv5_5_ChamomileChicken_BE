package jabaclass.payment.presentation.controller;

import jabaclass.payment.application.usercase.PaymentUseCase;
import jabaclass.payment.presentation.dto.request.PreparePaymentRequestDto;
import jabaclass.payment.presentation.dto.response.PaymentResponseDto;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/payments")
public class PaymentController implements PaymentApi {

	private final PaymentUseCase paymentUseCase;

	@Override
	@PostMapping("/prepare")
	public ResponseEntity<PaymentResponseDto> preparePayment(@RequestBody PreparePaymentRequestDto request) {
		PaymentResponseDto response = paymentUseCase.create(request);

		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}
}
