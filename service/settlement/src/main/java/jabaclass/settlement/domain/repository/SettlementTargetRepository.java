package jabaclass.settlement.domain.repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import jabaclass.settlement.domain.model.SettlementTarget;
import jabaclass.settlement.domain.model.SettlementTargetCalculationStatus;
import jabaclass.settlement.domain.model.SettlementTargetType;

public interface SettlementTargetRepository {

	SettlementTarget save(SettlementTarget settlementTarget);

	List<SettlementTarget> saveAll(List<SettlementTarget> settlementTargets);

	boolean existsByPaymentId(UUID paymentId);

	boolean existsByRefundId(UUID refundId);

	Optional<SettlementTarget> findByPaymentIdAndTargetType(UUID paymentId, SettlementTargetType targetType);

	List<SettlementTarget> findBySettlementMonth(String settlementMonth);

	List<SettlementTarget> findBySettlementMonthAndSellerId(String settlementMonth, UUID sellerId);

	List<SettlementTarget> findAllByIds(List<UUID> ids);

	List<SettlementTarget> findBySettlementMonthAndCalculationStatus(
		String settlementMonth,
		SettlementTargetCalculationStatus calculationStatus
	);

	BigDecimal sumGrossAmountBySellerIdAndSettlementMonths(UUID sellerId, List<String> settlementMonths);
}
