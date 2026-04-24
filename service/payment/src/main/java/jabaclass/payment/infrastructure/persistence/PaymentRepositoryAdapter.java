package jabaclass.payment.infrastructure.persistence;

import jabaclass.payment.domain.model.Payment;
import jabaclass.payment.domain.model.PaymentStatus;
import jabaclass.payment.domain.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Repository;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.Comparator;
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
	public Optional<Payment> findReusableByOrderId(UUID orderId) {
		return selectPreferredPayment(
			paymentJpaRepository.findAllByOrderIdOrderByCreatedAtDesc(orderId),
			List.of(PaymentStatus.PAID, PaymentStatus.READY)
		);
	}

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
		List<PaymentJpaRepository.SettlementPaymentSourceProjection> sources =
			(cursorOccurredAt == null || cursorId == null)
				? paymentJpaRepository.findSettlementPaymentSourcesWithoutCursor(
					from,
					to,
					PageRequest.of(0, size)
				)
				: paymentJpaRepository.findSettlementPaymentSourcesWithCursor(
					from,
					to,
					cursorOccurredAt,
					cursorId,
					PageRequest.of(0, size)
				);

		return sources.stream()
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

	@Override
	public Optional<Payment> findByOrderId(UUID orderId) {
		return selectPreferredPayment(
			paymentJpaRepository.findAllByOrderIdOrderByCreatedAtDesc(orderId),
			List.of(
				PaymentStatus.PAID,
				PaymentStatus.CANCELLED,
				PaymentStatus.READY,
				PaymentStatus.FAILED,
				PaymentStatus.EXPIRED
			)
		);
	}

	private Optional<Payment> selectPreferredPayment(List<Payment> payments, List<PaymentStatus> statusPriority) {
		return payments.stream()
			.min(
				Comparator
					.comparingInt((Payment payment) -> priorityOf(payment.getStatus(), statusPriority))
					.thenComparing(Payment::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder()))
			);
	}

	private int priorityOf(PaymentStatus status, List<PaymentStatus> statusPriority) {
		int priority = statusPriority.indexOf(status);
		return priority >= 0 ? priority : Integer.MAX_VALUE;
	}
}
