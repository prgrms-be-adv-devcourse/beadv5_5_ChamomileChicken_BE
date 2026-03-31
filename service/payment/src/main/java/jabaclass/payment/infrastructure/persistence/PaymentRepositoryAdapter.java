package jabaclass.payment.infrastructure.persistence;

import jabaclass.payment.domain.model.Payment;
import jabaclass.payment.domain.model.PaymentStatus;
import jabaclass.payment.domain.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Repository;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class PaymentRepositoryAdapter implements PaymentRepository {

	private final PaymentJpaRepository paymentJpaRepository;

	@Override
	public Payment save(Payment payment) {
		return paymentJpaRepository.save(payment);
	}

	@Override
	public Optional<Payment> findById(UUID id) {
		return paymentJpaRepository.findById(id);
	}

	@Override
	public Optional<Payment> findByOrderId(UUID orderId) { return paymentJpaRepository.findByOrderId(orderId); }

	@Override
	public List<Payment> findByStatusAndCreatedAtBefore(PaymentStatus status, LocalDateTime threshold) {
		return paymentJpaRepository.findByStatusAndCreatedAtBefore(
			status,
			threshold
		);
	}

	@Override
	public List<PaymentRepository.SettlementPaymentSource> findSettlementPaymentSources(
		LocalDateTime from,
		LocalDateTime to,
		LocalDateTime cursorOccurredAt,
		UUID cursorId,
		int size
	) {
		return paymentJpaRepository.findSettlementPaymentSources(
			from,
			to,
			cursorOccurredAt,
			cursorId,
			PageRequest.of(0, size)
		).stream()
			.map(it -> new PaymentRepository.SettlementPaymentSource(
				it.getPaymentId(),
				it.getOrderId(),
				it.getProductId(),
				it.getPaymentStatus(),
				it.getTotalPaymentAmount(),
				it.getOccurredAt()
			))
			.toList();
	}
}
