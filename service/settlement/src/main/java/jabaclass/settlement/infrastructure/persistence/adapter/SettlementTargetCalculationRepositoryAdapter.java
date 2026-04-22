package jabaclass.settlement.infrastructure.persistence.adapter;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import jabaclass.settlement.application.dto.SettlementTargetSummary;
import jabaclass.settlement.domain.model.settlement.SettlementTargetCalculation;
import jabaclass.settlement.domain.repository.SettlementTargetCalculationRepository;
import jabaclass.settlement.infrastructure.persistence.SettlementTargetCalculationJpaRepository;
import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class SettlementTargetCalculationRepositoryAdapter implements SettlementTargetCalculationRepository {

	private final SettlementTargetCalculationJpaRepository settlementTargetCalculationJpaRepository;

	@Override
	public List<SettlementTargetCalculation> saveAll(List<SettlementTargetCalculation> settlementTargetCalculations) {
		return settlementTargetCalculationJpaRepository.saveAll(settlementTargetCalculations);
	}

	@Override
	public boolean existsBySettlementTargetId(UUID settlementTargetId) {
		return settlementTargetCalculationJpaRepository.existsBySettlementTargetId(settlementTargetId);
	}

	@Override
	public Optional<SettlementTargetCalculation> findBySettlementTargetId(UUID settlementTargetId) {
		return settlementTargetCalculationJpaRepository.findBySettlementTargetId(settlementTargetId);
	}

	@Override
	public List<SettlementTargetCalculation> findBySettlementMonthAndSellerId(String settlementMonth, UUID sellerId) {
		return settlementTargetCalculationJpaRepository.findBySettlementMonthAndSellerId(settlementMonth, sellerId);
	}

	@Override
	public List<SettlementTargetCalculation> findBySettlementMonthAndSellerIds(
		String settlementMonth,
		List<UUID> sellerIds
	) {
		if (sellerIds == null || sellerIds.isEmpty()) {
			return List.of();
		}

		return settlementTargetCalculationJpaRepository.findBySettlementMonthAndSellerIdIn(settlementMonth, sellerIds);
	}

	@Override
	public Page<SettlementTargetCalculation> findBySettlementMonthAndSellerId(
		String settlementMonth,
		UUID sellerId,
		Pageable pageable
	) {
		return settlementTargetCalculationJpaRepository.findBySettlementMonthAndSellerId(
			settlementMonth,
			sellerId,
			pageable
		);
	}

	@Override
	public List<SettlementTargetSummary> findSummaryBySettlementMonth(String settlementMonth) {
		return settlementTargetCalculationJpaRepository.findSummaryBySettlementMonth(settlementMonth)
			.stream()
			.map(it -> new SettlementTargetSummary(
				it.getSellerId(),
				it.getSettlementMonth(),
				it.getTotalSettlementBaseAmount()
			))
			.toList();
	}
}
