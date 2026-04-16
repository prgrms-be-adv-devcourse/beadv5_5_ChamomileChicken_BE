package jabaclass.payment.application.service.handler;

import java.math.BigDecimal;
import java.util.UUID;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;

import jabaclass.payment.common.error.PaymentErrorCode;
import jabaclass.payment.common.error.PaymentException;
import jabaclass.payment.domain.model.Payment;
import jabaclass.payment.domain.repository.PaymentRepository;
import jabaclass.payment.infrastructure.kafka.PaymentCompletedEvent;
import jabaclass.payment.infrastructure.kafka.PaymentFailedEvent;
import jabaclass.payment.infrastructure.outbox.EventType;
import jabaclass.payment.infrastructure.outbox.OutboxEvent;
import jabaclass.payment.infrastructure.outbox.OutboxRepository;
import jabaclass.payment.presentation.dto.response.PaymentResponseDto;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class PaymentConfirmHandler {

	private final PaymentRepository paymentRepository;
	private final OutboxRepository outboxRepository;
	private final ObjectMapper objectMapper;

	@Transactional
	public PaymentResponseDto onSuccess(UUID paymentId, String paymentKey) {
		Payment payment = findPaymentOrThrow(paymentId);
		payment.markDone(paymentKey);
		outboxRepository.save(OutboxEvent.create(
			"PAYMENT",
			paymentId.toString(),
			EventType.PAYMENT_COMPLETED,
			toJson(new PaymentCompletedEvent(UUID.randomUUID(), paymentId, payment.getOrderId()))
		));
		return PaymentResponseDto.from(payment);
	}

	@Transactional
	public void onFailure(UUID paymentId, UUID orderId, BigDecimal depositAmount) {
		Payment payment = findPaymentOrThrow(paymentId);
		payment.markFailed();
		outboxRepository.save(OutboxEvent.create(
			"PAYMENT",
			paymentId.toString(),
			EventType.PAYMENT_FAILED,
			toJson(new PaymentFailedEvent(UUID.randomUUID(), paymentId, orderId, depositAmount))
		));
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
