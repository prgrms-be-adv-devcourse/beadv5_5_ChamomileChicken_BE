package jabaclass.payment.infrastructure.persistence;

import jabaclass.payment.domain.model.Payment;
import jabaclass.payment.domain.model.PaymentStatus;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface PaymentJpaRepository extends JpaRepository<Payment, UUID> {
	List<Payment> findAllByOrderIdOrderByCreatedAtDesc(UUID orderId);

	List<Payment> findByStatusAndCreatedAtBefore(
		PaymentStatus status,
		LocalDateTime threshold
	);

	@Query("""
		select
			p.id as paymentId,
			p.orderId as orderId,
			p.productId as productId,
			'PAID' as paymentStatus,
			p.totalAmount as totalPaymentAmount,
			p.paidAt as occurredAt
		from Payment p
		where p.paidAt is not null
			and p.paidAt >= :from
			and p.paidAt <= :to
		order by p.paidAt asc, p.id asc
		""")
	List<SettlementPaymentSourceProjection> findSettlementPaymentSourcesWithoutCursor(
		LocalDateTime from,
		LocalDateTime to,
		Pageable pageable
	);

	@Query("""
		select
			p.id as paymentId,
			p.orderId as orderId,
			p.productId as productId,
			'PAID' as paymentStatus,
			p.totalAmount as totalPaymentAmount,
			p.paidAt as occurredAt
		from Payment p
		where p.paidAt is not null
			and p.paidAt >= :from
			and p.paidAt <= :to
			and (
				p.paidAt > :cursorOccurredAt
				or (p.paidAt = :cursorOccurredAt and p.id > :cursorId)
			)
		order by p.paidAt asc, p.id asc
		""")
	List<SettlementPaymentSourceProjection> findSettlementPaymentSourcesWithCursor(
		LocalDateTime from,
		LocalDateTime to,
		LocalDateTime cursorOccurredAt,
		UUID cursorId,
		Pageable pageable
	);

	interface SettlementPaymentSourceProjection {
		UUID getPaymentId();
		UUID getOrderId();
		UUID getProductId();
		String getPaymentStatus();
		java.math.BigDecimal getTotalPaymentAmount();
		LocalDateTime getOccurredAt();
	}
}
