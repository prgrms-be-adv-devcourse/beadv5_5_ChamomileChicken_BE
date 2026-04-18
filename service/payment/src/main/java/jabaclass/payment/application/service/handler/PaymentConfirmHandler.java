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

	// [핸들러] PG 성공 확정 후 호출 — Payment PAID 상태 변경 + Outbox 저장을 원자적으로 커밋
	// Outbox에 저장된 PAYMENT_COMPLETED는 OutboxPublisher가 폴링해 Kafka로 발행 → Order가 소비
	@Transactional
	public PaymentResponseDto onSuccess(UUID paymentId, String paymentKey) {
		Payment payment = findPaymentOrThrow(paymentId);
		// 이미 PAID면 Outbox 중복 저장 방지
		if (payment.isDone()) {
			return PaymentResponseDto.from(payment);
		}
		payment.markDone(paymentKey);
		outboxRepository.save(OutboxEvent.create(
			"PAYMENT",
			paymentId.toString(),
			EventType.PAYMENT_COMPLETED,
			toJson(new PaymentCompletedEvent(UUID.randomUUID(), paymentId, payment.getOrderId()))
		));
		return PaymentResponseDto.from(payment);
	}

	// [핸들러] PG 실패 또는 예외 발생 후 호출 — Payment FAILED + Outbox PAYMENT_FAILED 저장
	// Order가 PAYMENT_FAILED 수신 → 재고 복구 + 예치금 복구 보상 이벤트 발행 (Saga 보상 트랜잭션)
	@Transactional
	public void onFailure(UUID paymentId, UUID orderId, BigDecimal depositAmount) {
		Payment payment = findPaymentOrThrow(paymentId);
		// 이미 종료 상태면 중복 Outbox 저장 방지
		if (payment.getStatus() == PaymentStatus.FAILED
			|| payment.getStatus() == PaymentStatus.EXPIRED
			|| payment.getStatus() == PaymentStatus.CANCELLED) {
			return;
		}
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
