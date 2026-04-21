package jabaclass.payment.domain.repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import jabaclass.payment.domain.model.Refund;

public interface RefundRepository {
	Refund save(Refund refund);

	Optional<Refund> findLatestCompletedByPaymentId(UUID paymentId);

	List<SettlementRefundSource> findSettlementRefundSources(
		LocalDateTime from,
		LocalDateTime to,
		LocalDateTime cursorOccurredAt,
		UUID cursorId,
		int size
	);

	record SettlementRefundSource(
		UUID refundId,
		UUID paymentId,
		UUID orderId,
		UUID productId,
		String refundStatus,
		BigDecimal totalRefundAmount,
		LocalDateTime occurredAt
	) {
	}
}
