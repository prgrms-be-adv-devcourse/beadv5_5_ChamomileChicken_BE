package jabaclass.payment.application.service.handler;

import java.math.BigDecimal;
import java.util.UUID;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;

import jabaclass.payment.common.error.PaymentErrorCode;
import jabaclass.payment.common.error.PaymentException;
import jabaclass.payment.domain.model.Payment;
import jabaclass.payment.domain.model.PaymentStatus;
import jabaclass.payment.domain.model.Refund;
import jabaclass.payment.domain.repository.PaymentRepository;
import jabaclass.payment.domain.repository.RefundRepository;
import jabaclass.payment.presentation.dto.response.InternalRefundResponseDto;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class PaymentRefundHandler {

	private final PaymentRepository paymentRepository;
	private final RefundRepository refundRepository;

	// [핸들러] PG 환불 성공 확정 후 호출 — Payment CANCELLED + Refund 생성을 원자적으로 커밋
	@Transactional
	public InternalRefundResponseDto onSuccess(UUID paymentId, BigDecimal refundRate) {
		Payment payment = findPaymentOrThrow(paymentId);
		// 이미 CANCELLED면 Outbox 중복 저장 방지, depositRefundAmount만 반환
		if (payment.getStatus() == PaymentStatus.CANCELLED) {
			Refund refund = refundRepository.findByPaymentId(paymentId)
				.orElseThrow(() -> new PaymentException(PaymentErrorCode.PAYMENT_REFUND_FAILED));
			return toResponse(refund, payment);
		}
		payment.markCancelled();

		BigDecimal paymentRefundAmount = payment.getPaymentAmount().multiply(refundRate);
		BigDecimal depositRefundAmount = payment.getDepositAmount().multiply(refundRate);

		Refund refund = Refund.create(
			paymentId,
			null,
			payment.getPaymentAmount(),
			payment.getDepositAmount(),
			refundRate,
			paymentRefundAmount,
			depositRefundAmount
		);
		refund.markCompleted();
		refundRepository.save(refund);

		return toResponse(refund, payment);
	}

	private InternalRefundResponseDto toResponse(Refund refund, Payment payment) {
		return new InternalRefundResponseDto(
			refund.getId(),
			payment.getId(),
			payment.getProductId(),
			refund.getDepositRefundAmount(),
			refund.getTotalRefundAmount(),
			refund.getProcessedAt()
		);
	}

	private Payment findPaymentOrThrow(UUID paymentId) {
		return paymentRepository.findById(paymentId)
			.orElseThrow(() -> new PaymentException(PaymentErrorCode.PAYMENT_NOT_FOUND));
	}
}
