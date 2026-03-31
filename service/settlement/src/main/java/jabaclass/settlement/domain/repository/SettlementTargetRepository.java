package jabaclass.settlement.domain.repository;

import java.util.List;
import java.util.UUID;

import jabaclass.settlement.application.dto.SettlementTargetSummary;
import jabaclass.settlement.domain.model.SettlementTarget;

public interface SettlementTargetRepository {

	SettlementTarget save(SettlementTarget settlementTarget);

	List<SettlementTarget> saveAll(List<SettlementTarget> settlementTargets);

	boolean existsByPaymentId(UUID paymentId);

	boolean existsByRefundId(UUID refundId);

	List<SettlementTarget> findBySettlementMonth(String settlementMonth);

	List<SettlementTarget> findBySettlementMonthAndSellerId(String settlementMonth, UUID sellerId);

	List<SettlementTargetSummary> findSummaryBySettlementMonth(String settlementMonth);
}
