package jabaclass.payment.presentation.controller;

import jabaclass.payment.application.usecase.DepositPaymentUseCase;
import jabaclass.payment.presentation.dto.request.ConfirmDepositPaymentRequestDto;
import jabaclass.payment.presentation.dto.request.PrepareDepositPaymentRequestDto;
import jabaclass.payment.presentation.dto.response.ConfirmDepositPaymentResponseDto;
import jabaclass.payment.presentation.dto.response.PrepareDepositPaymentResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/payments/deposits")
public class DepositPaymentController implements DepositPaymentApi {

	private final DepositPaymentUseCase depositPaymentUseCase;

	@Override
	@PostMapping("/prepare")
	public ResponseEntity<PrepareDepositPaymentResponseDto> prepareDepositPayment(
		@RequestBody PrepareDepositPaymentRequestDto request
	) {
		PrepareDepositPaymentResponseDto response = depositPaymentUseCase.prepare(request);
		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}

	@Override
	@PostMapping("/confirm")
	public ResponseEntity<ConfirmDepositPaymentResponseDto> confirmDepositPayment(
		@RequestBody ConfirmDepositPaymentRequestDto request
	) {
		ConfirmDepositPaymentResponseDto response = depositPaymentUseCase.confirm(request);
		return ResponseEntity.ok(response);
	}
}
