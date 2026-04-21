package jabaclass.payment.presentation.controller;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jabaclass.payment.application.usecase.PaymentSettlementQueryUseCase;
import jabaclass.payment.application.usecase.PaymentUseCase;
import jabaclass.payment.presentation.dto.request.InternalRefundRequestDto;
import jabaclass.payment.presentation.dto.response.InternalRefundDetailResponseDto;
import jabaclass.payment.presentation.dto.response.InternalRefundResponseDto;
import jabaclass.payment.presentation.dto.response.PaymentSettlementSliceResponseDto;
import jabaclass.payment.presentation.dto.response.RefundSettlementSliceResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentInternalController implements PaymentInternalApi {

	private final PaymentSettlementQueryUseCase paymentSettlementQueryUseCase;
	private final PaymentUseCase paymentUseCase;

	@PostMapping("/internal/refunds")
	public InternalRefundResponseDto refundByOrder(@RequestBody InternalRefundRequestDto request) {
		return paymentUseCase.refundByOrder(request);
	}

	@Override
	@GetMapping("/internal/refunds/orders/{orderId}")
	public InternalRefundDetailResponseDto getRefundInfoByOrder(@PathVariable UUID orderId) {
		return paymentUseCase.getRefundInfoByOrder(orderId);
	}

	@Override
	@GetMapping("/settlement-targets")
	public PaymentSettlementSliceResponseDto getPaymentSettlementTargets(
		@RequestParam LocalDateTime from,
		@RequestParam LocalDateTime to,
		@RequestParam(required = false) LocalDateTime cursorUpdatedAt,
		@RequestParam(required = false) UUID cursorId,
		@RequestParam(defaultValue = "1000") int size
	) {
		return paymentSettlementQueryUseCase.getPaymentSettlementTargets(from, to, cursorUpdatedAt, cursorId, size);
	}

	@Override
	@GetMapping("/refunds/settlement-targets")
	public RefundSettlementSliceResponseDto getRefundSettlementTargets(
		@RequestParam LocalDateTime from,
		@RequestParam LocalDateTime to,
		@RequestParam(required = false) LocalDateTime cursorUpdatedAt,
		@RequestParam(required = false) UUID cursorId,
		@RequestParam(defaultValue = "1000") int size
	) {
		return paymentSettlementQueryUseCase.getRefundSettlementTargets(from, to, cursorUpdatedAt, cursorId, size);
	}
}
