package jabaclass.payment.application.service;

import java.time.LocalDateTime;
import java.util.List;
import jabaclass.payment.application.usecase.ExpirePaymentUseCase;
import jabaclass.payment.domain.model.Payment;
import jabaclass.payment.domain.model.PaymentStatus;
import jabaclass.payment.domain.repository.PaymentRepository;
import jabaclass.payment.infrastructure.kafka.PaymentExpiredEvent;
import jabaclass.payment.infrastructure.kafka.PaymentExpiredEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExpirePaymentService implements ExpirePaymentUseCase {

	private static final long EXPIRE_MINUTES = 10L;

	private final PaymentRepository paymentRepository;
	private final PaymentExpiredEventPublisher paymentExpiredEventPublisher;

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

				paymentExpiredEventPublisher.publish(
					new PaymentExpiredEvent(payment.getId(), payment.getOrderId())
				);

				log.info("결제 만료 처리 완료. paymentId={}, orderId={}",
					payment.getId(), payment.getOrderId());

			} catch (Exception e) {
				log.error("결제 만료 처리 실패. paymentId={}, orderId={}",
					payment.getId(), payment.getOrderId(), e);
			}
		}
	}
}