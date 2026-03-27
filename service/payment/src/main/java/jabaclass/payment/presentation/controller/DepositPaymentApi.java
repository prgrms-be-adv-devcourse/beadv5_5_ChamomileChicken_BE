package jabaclass.payment.presentation.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jabaclass.payment.presentation.dto.request.ConfirmDepositPaymentRequestDto;
import jabaclass.payment.presentation.dto.request.PrepareDepositPaymentRequestDto;
import jabaclass.payment.presentation.dto.response.ConfirmDepositPaymentResponseDto;
import jabaclass.payment.presentation.dto.response.PrepareDepositPaymentResponseDto;
import org.springframework.http.ResponseEntity;

@Tag(name = "DepositPayment", description = "예치금 결제 API")
public interface DepositPaymentApi {

	@Operation(summary = "예치금 결제 준비", description = "예치금 충전을 위한 결제를 생성하고 READY 상태로 만듭니다.")
	ResponseEntity<PrepareDepositPaymentResponseDto> prepareDepositPayment(PrepareDepositPaymentRequestDto request);

	@Operation(summary = "예치금 결제 확정", description = "예치금 결제를 DONE 상태로 확정합니다.")
	ResponseEntity<ConfirmDepositPaymentResponseDto> confirmDepositPayment(ConfirmDepositPaymentRequestDto request);
}
