package jabaclass.settlement.domain.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import jabaclass.settlement.domain.model.settlement.SettlementTarget;
import jabaclass.settlement.domain.model.settlement.SettlementTargetCalculationStatus;
import jabaclass.settlement.domain.model.settlement.SettlementTargetType;
import jabaclass.settlement.application.dto.SellerSalesAmount;

public interface SettlementTargetRepository {

	List<SettlementTarget> saveAll(List<SettlementTarget> settlementTargets);

	SettlementTarget save(SettlementTarget settlementTarget);

	Optional<SettlementTarget> findByPaymentIdAndTargetType(UUID paymentId, SettlementTargetType targetType);

	List<SettlementTarget> findByPaymentIdsAndTargetType(List<UUID> paymentIds, SettlementTargetType targetType);

	Optional<SettlementTarget> findByRefundId(UUID refundId);

	List<SettlementTarget> findAllByIds(List<UUID> ids);

	List<SettlementTarget> findBySettlementMonthAndCalculationStatus(
		String settlementMonth,
		SettlementTargetCalculationStatus calculationStatus
	);

	List<SellerSalesAmount> sumSettlementBaseAmountBySellerIdsAndSettlementMonths(
		List<UUID> sellerIds,
		List<String> settlementMonths
	);
}
