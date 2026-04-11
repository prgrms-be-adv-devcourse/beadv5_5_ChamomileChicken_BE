package jabaclass.payment.application.service;

import java.time.LocalDateTime;
import java.util.List;
import jabaclass.payment.application.usecase.ExpirePaymentUseCase;
import jabaclass.payment.domain.model.Payment;
import jabaclass.payment.domain.model.PaymentStatus;
import jabaclass.payment.domain.repository.PaymentRepository;
import jabaclass.payment.infrastructure.kafka.PaymentExpiredEvent;
import jabaclass.payment.infrastructure.outbox.EventType;
import jabaclass.payment.infrastructure.outbox.OutboxEvent;
import jabaclass.payment.infrastructure.outbox.OutboxRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExpirePaymentService implements ExpirePaymentUseCase {

	private static final long EXPIRE_MINUTES = 10L;

	private final PaymentRepository paymentRepository;
	private final OutboxRepository outboxRepository;
	private final ObjectMapper objectMapper;

	@Override
	@Transactional
	public void execute() {
		LocalDateTime threshold = LocalDateTime.now().minusMinutes(EXPIRE_MINUTES);

		List<Payment> expiredTargets = paymentRepository.findByStatusAndCreatedAtBefore(
			PaymentStatus.READY,
			threshold
		);

		log.info("결제 만료 대상 수: {}", expiredTargets.size());

		for (Payment payment : expiredTargets) {
			try {
				payment.expire();

				outboxRepository.save(
					OutboxEvent.create(
						"PAYMENT",
						payment.getId().toString(),
						EventType.PAYMENT_EXPIRED,
						toJson(new PaymentExpiredEvent(payment.getId(), payment.getOrderId()))
					)
				);

				log.info("결제 만료 처리 완료. paymentId={}, orderId={}",
					payment.getId(), payment.getOrderId());

			} catch (Exception e) {
				log.error("결제 만료 처리 실패. paymentId={}, orderId={}",
					payment.getId(), payment.getOrderId(), e);
			}
		}
	}

	private String toJson(Object obj) {
		try {
			return objectMapper.writeValueAsString(obj);
		} catch (Exception e) {
			throw new RuntimeException("Outbox 직렬화 실패", e);
		}
	}
}