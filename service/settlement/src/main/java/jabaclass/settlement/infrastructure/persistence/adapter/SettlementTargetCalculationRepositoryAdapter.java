package jabaclass.settlement.infrastructure.persistence.adapter;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import jabaclass.settlement.application.dto.SettlementTargetSummary;
import jabaclass.settlement.domain.model.SettlementTargetCalculation;
import jabaclass.settlement.domain.repository.SettlementTargetCalculationRepository;
import jabaclass.settlement.infrastructure.persistence.SettlementTargetCalculationJpaRepository;
import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class SettlementTargetCalculationRepositoryAdapter implements SettlementTargetCalculationRepository {

	private final SettlementTargetCalculationJpaRepository settlementTargetCalculationJpaRepository;

	@Override
	public SettlementTargetCalculation save(SettlementTargetCalculation settlementTargetCalculation) {
		return settlementTargetCalculationJpaRepository.save(settlementTargetCalculation);
	}

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
	public List<SettlementTargetCalculation> findBySettlementMonth(String settlementMonth) {
		return settlementTargetCalculationJpaRepository.findBySettlementMonth(settlementMonth);
	}

	@Override
	public List<SettlementTargetCalculation> findBySettlementMonthAndSellerId(String settlementMonth, UUID sellerId) {
		return settlementTargetCalculationJpaRepository.findBySettlementMonthAndSellerId(settlementMonth, sellerId);
	}

	@Override
	public List<SettlementTargetSummary> findSummaryBySettlementMonth(String settlementMonth) {
		return settlementTargetCalculationJpaRepository.findSummaryBySettlementMonth(settlementMonth)
			.stream()
			.map(it -> new SettlementTargetSummary(
				it.getSellerId(),
				it.getSettlementMonth(),
				it.getTotalSettlementBaseAmount(),
				it.getTotalFeeAmount(),
				it.getTotalSettlementAmount(),
				it.getTargetCount()
			))
			.toList();
	}

	@Override
	public BigDecimal sumSettlementBaseAmountBySellerIdAndSettlementMonths(UUID sellerId, List<String> settlementMonths) {
		if (settlementMonths == null || settlementMonths.isEmpty()) {
			return BigDecimal.ZERO;
		}

		return settlementTargetCalculationJpaRepository.sumSettlementBaseAmountBySellerIdAndSettlementMonths(
			sellerId,
			settlementMonths
		);
	}
}
