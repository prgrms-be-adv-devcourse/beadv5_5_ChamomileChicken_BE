package jabaclass.payment.infrastructure.persistence;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import jabaclass.payment.domain.model.Refund;
import jabaclass.payment.domain.repository.RefundRepository;
import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class RefundRepositoryAdapter implements RefundRepository {

	private final RefundJpaRepository refundJpaRepository;

	@Override
	public Refund save(Refund refund) {
		return refundJpaRepository.save(refund);
	}

	@Override
	public Optional<Refund> findByPaymentId(UUID paymentId) {
		return refundJpaRepository.findByPaymentId(paymentId);
	}

	@Override
	public List<RefundRepository.SettlementRefundSource> findSettlementRefundSources(
		LocalDateTime from,
		LocalDateTime to,
		LocalDateTime cursorOccurredAt,
		UUID cursorId,
		int size
	) {
		List<RefundJpaRepository.SettlementRefundSourceProjection> sources =
			(cursorOccurredAt == null || cursorId == null)
				? refundJpaRepository.findSettlementRefundSourcesWithoutCursor(
					from,
					to,
					PageRequest.of(0, size)
				)
				: refundJpaRepository.findSettlementRefundSourcesWithCursor(
					from,
					to,
					cursorOccurredAt,
					cursorId,
					PageRequest.of(0, size)
				);

		return sources.stream()
			.map(it -> new RefundRepository.SettlementRefundSource(
				it.getRefundId(),
				it.getPaymentId(),
				it.getOrderId(),
				it.getProductId(),
				it.getRefundStatus().name(),
				it.getTotalRefundAmount(),
				it.getOccurredAt()
			))
			.toList();
	}
}
