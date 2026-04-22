package jabaclass.settlement.infrastructure.persistence.adapter;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import jabaclass.settlement.application.dto.SellerSalesAmount;
import jabaclass.settlement.domain.model.settlement.SettlementTarget;
import jabaclass.settlement.domain.model.settlement.SettlementTargetCalculationStatus;
import jabaclass.settlement.domain.model.settlement.SettlementTargetType;
import jabaclass.settlement.domain.repository.SettlementTargetRepository;
import jabaclass.settlement.infrastructure.persistence.SettlementTargetJpaRepository;
import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class SettlementTargetRepositoryAdapter implements SettlementTargetRepository {

	private final SettlementTargetJpaRepository settlementTargetJpaRepository;

	@Override
	public List<SettlementTarget> saveAll(List<SettlementTarget> settlementTargets) {
		return settlementTargetJpaRepository.saveAll(settlementTargets);
	}

	@Override
	public SettlementTarget save(SettlementTarget settlementTarget) {
		return settlementTargetJpaRepository.save(settlementTarget);
	}

	@Override
	public Optional<SettlementTarget> findByPaymentIdAndTargetType(UUID paymentId, SettlementTargetType targetType) {
		return settlementTargetJpaRepository.findByPaymentIdAndTargetType(paymentId, targetType);
	}

	@Override
	public Optional<SettlementTarget> findByRefundId(UUID refundId) {
		return settlementTargetJpaRepository.findByRefundId(refundId);
	}

	@Override
	public List<SettlementTarget> findAllByIds(List<UUID> ids) {
		if (ids == null || ids.isEmpty()) {
			return List.of();
		}

		return settlementTargetJpaRepository.findByIdIn(ids);
	}

	@Override
	public List<SettlementTarget> findBySettlementMonthAndCalculationStatus(
		String settlementMonth,
		SettlementTargetCalculationStatus calculationStatus
	) {
		return settlementTargetJpaRepository.findBySettlementMonthAndCalculationStatus(settlementMonth, calculationStatus);
	}

	@Override
	public List<SellerSalesAmount> sumSettlementBaseAmountBySellerIdsAndSettlementMonths(
		List<UUID> sellerIds,
		List<String> settlementMonths
	) {
		if (sellerIds == null || sellerIds.isEmpty() || settlementMonths == null || settlementMonths.isEmpty()) {
			return List.of();
		}

		return settlementTargetJpaRepository.sumSettlementBaseAmountBySellerIdsAndSettlementMonths(
				sellerIds,
				settlementMonths
			).stream()
			.map(it -> new SellerSalesAmount(it.getSellerId(), it.getSalesAmount()))
			.toList();
	}
}
