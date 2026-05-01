package jabaclass.settlement.infrastructure.persistence.adapter;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import jabaclass.settlement.application.dto.SellerSalesAmount;
import jabaclass.settlement.application.dto.SettlementFeeRateAmount;
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
	public List<SettlementTargetCalculation> findBySettlementTargetIds(List<UUID> settlementTargetIds) {
		if (settlementTargetIds == null || settlementTargetIds.isEmpty()) {
			return List.of();
		}

		return settlementTargetCalculationJpaRepository.findBySettlementTargetIdIn(settlementTargetIds);
	}

	@Override
	public List<SellerSalesAmount> sumSettlementBaseAmountBySettlementMonths(List<String> settlementMonths) {
		if (settlementMonths == null || settlementMonths.isEmpty()) {
			return List.of();
		}

		return settlementTargetCalculationJpaRepository.sumSettlementBaseAmountBySettlementMonths(settlementMonths)
			.stream()
			.map(it -> new SellerSalesAmount(it.getSellerId(), it.getSalesAmount()))
			.toList();
	}

	@Override
	public List<SettlementFeeRateAmount> sumSettlementBaseAmountBySettlementMonthGroupedBySellerAndFeeRate(
		String settlementMonth
	) {
		return settlementTargetCalculationJpaRepository.sumSettlementBaseAmountBySettlementMonthGroupedBySellerAndFeeRate(
				settlementMonth
			).stream()
			.map(it -> new SettlementFeeRateAmount(
				it.getSellerId(),
				it.getAppliedFeeRate(),
				it.getSettlementBaseAmount()
			))
			.toList();
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
}
