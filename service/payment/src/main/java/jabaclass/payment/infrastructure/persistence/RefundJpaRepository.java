package jabaclass.payment.infrastructure.persistence;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import jabaclass.payment.domain.model.Refund;

public interface RefundJpaRepository extends JpaRepository<Refund, UUID> {

	@Query("""
		select
			r.id as refundId,
			r.paymentId as paymentId,
			p.orderId as orderId,
			p.productId as productId,
			r.status as refundStatus,
			r.totalRefundAmount as totalRefundAmount,
			r.processedAt as occurredAt
		from Refund r
		join Payment p on p.id = r.paymentId
		where r.status = jabaclass.payment.domain.model.RefundStatus.COMPLETED
			and r.processedAt is not null
			and r.processedAt >= :from
			and r.processedAt <= :to
			and (
				:cursorOccurredAt is null
				or r.processedAt > :cursorOccurredAt
				or (r.processedAt = :cursorOccurredAt and r.id > :cursorId)
			)
		order by r.processedAt asc, r.id asc
		""")
	java.util.List<SettlementRefundSourceProjection> findSettlementRefundSources(
		LocalDateTime from,
		LocalDateTime to,
		LocalDateTime cursorOccurredAt,
		UUID cursorId,
		Pageable pageable
	);

	interface SettlementRefundSourceProjection {
		UUID getRefundId();
		UUID getPaymentId();
		UUID getOrderId();
		UUID getProductId();
		jabaclass.payment.domain.model.RefundStatus getRefundStatus();
		java.math.BigDecimal getTotalRefundAmount();
		java.time.LocalDateTime getOccurredAt();
	}
}
