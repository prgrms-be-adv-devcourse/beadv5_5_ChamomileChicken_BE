package jabaclass.payment.application.service.handler;

import java.math.BigDecimal;
import java.util.UUID;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;

import jabaclass.payment.common.error.PaymentErrorCode;
import jabaclass.payment.common.error.PaymentException;
import jabaclass.payment.domain.model.Payment;
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

	@Transactional
	public BigDecimal onSuccess(UUID paymentId, BigDecimal refundRate) {
		Payment payment = findPaymentOrThrow(paymentId);
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

		outboxRepository.save(OutboxEvent.create(
			"PAYMENT",
			paymentId.toString(),
			EventType.PAYMENT_REFUNDED,
			toJson(new PaymentRefundedEvent(UUID.randomUUID(), paymentId, payment.getOrderId()))
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