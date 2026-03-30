package jabaclass.settlement.infrastructure.persistence.adapter;

import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

import jabaclass.settlement.application.dto.SettlementTargetSummary;
import jabaclass.settlement.domain.model.SettlementTarget;
import jabaclass.settlement.domain.repository.SettlementTargetRepository;
import jabaclass.settlement.infrastructure.persistence.SettlementTargetJpaRepository;
import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class SettlementTargetRepositoryAdapter implements SettlementTargetRepository {

	private final SettlementTargetJpaRepository settlementTargetJpaRepository;

	@Override
	public SettlementTarget save(SettlementTarget settlementTarget) {
		return settlementTargetJpaRepository.save(settlementTarget);
	}

	@Override
	public List<SettlementTarget> saveAll(List<SettlementTarget> settlementTargets) {
		return settlementTargetJpaRepository.saveAll(settlementTargets);
	}

	@Override
	public boolean existsByPaymentId(UUID paymentId) {
		return settlementTargetJpaRepository.existsByPaymentId(paymentId);
	}

	@Override
	public boolean existsByRefundId(UUID refundId) {
		return settlementTargetJpaRepository.existsByRefundId(refundId);
	}

	@Override
	public List<SettlementTarget> findBySettlementMonth(String settlementMonth) {
		return settlementTargetJpaRepository.findBySettlementMonth(settlementMonth);
	}

	@Override
	public List<SettlementTarget> findBySettlementMonthAndSellerId(String settlementMonth, UUID sellerId) {
		return settlementTargetJpaRepository.findBySettlementMonthAndSellerId(settlementMonth, sellerId);
	}

	@Override
	public List<SettlementTargetSummary> findSummaryBySettlementMonth(String settlementMonth) {
		return settlementTargetJpaRepository.findSummaryBySettlementMonth(settlementMonth)
			.stream()
			.map(it -> new SettlementTargetSummary(
				it.getSellerId(),
				it.getSettlementMonth(),
				it.getTotalSettlementAmount(),
				it.getTargetCount(),
				it.getOrderCount()
			))
			.toList();
	}
}
