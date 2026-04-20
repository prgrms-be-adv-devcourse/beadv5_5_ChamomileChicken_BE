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
import jabaclass.payment.domain.model.RefundStatus;
import jabaclass.payment.domain.repository.PaymentRepository;
import jabaclass.payment.domain.repository.RefundRepository;
import jabaclass.payment.infrastructure.kafka.PaymentRefundedEvent;
import jabaclass.payment.infrastructure.outbox.EventType;
import jabaclass.payment.infrastructure.outbox.OutboxEvent;
import jabaclass.payment.infrastructure.outbox.OutboxRepository;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class PaymentRefundHandler {

	private final PaymentRepository paymentRepository;
	private final RefundRepository refundRepository;
	private final OutboxRepository outboxRepository;
	private final ObjectMapper objectMapper;

	// [핸들러] PG 환불 성공 확정 후 호출 — Payment CANCELLED + Refund 생성 + Outbox 저장을 원자적으로 커밋
	// Outbox에 저장된 PAYMENT_REFUNDED는 OutboxPublisher가 폴링해 Kafka로 발행
	// Order가 PAYMENT_REFUNDED 수신 → Order REFUNDED 상태 변경 (동기 경로 실패 시 복구 경로로도 동작)
	@Transactional
	public BigDecimal onSuccess(UUID paymentId, BigDecimal refundRate) {
		Payment payment = findPaymentOrThrow(paymentId);
		// 이미 CANCELLED면 Outbox 중복 저장 방지, depositRefundAmount만 반환
		if (payment.getStatus() == PaymentStatus.CANCELLED) {
			return payment.getDepositAmount().multiply(refundRate);
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

		// Order에 환불 완료 전파 — depositRefundAmount 포함해 Order가 예치금 복구 이벤트 발행 가능하도록
		outboxRepository.save(OutboxEvent.create(
			"PAYMENT",
			paymentId.toString(),
			EventType.PAYMENT_REFUNDED,
			toJson(new PaymentRefundedEvent(UUID.randomUUID(), paymentId, payment.getOrderId(), depositRefundAmount))
		));

		return depositRefundAmount;
	}

	private Payment findPaymentOrThrow(UUID paymentId) {
		return paymentRepository.findById(paymentId)
			.orElseThrow(() -> new PaymentException(PaymentErrorCode.PAYMENT_NOT_FOUND));
	}

	private String toJson(Object obj) {
		try {
			return objectMapper.writeValueAsString(obj);
		} catch (Exception e) {
			throw new RuntimeException("Outbox 직렬화 실패", e);
		}
	}
}