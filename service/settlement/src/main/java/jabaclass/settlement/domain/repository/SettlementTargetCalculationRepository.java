package jabaclass.settlement.domain.repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import jabaclass.settlement.application.dto.SettlementTargetSummary;
import jabaclass.settlement.domain.model.SettlementTargetCalculation;

public interface SettlementTargetCalculationRepository {

	List<SettlementTargetCalculation> saveAll(List<SettlementTargetCalculation> settlementTargetCalculations);

	boolean existsBySettlementTargetId(UUID settlementTargetId);

	Optional<SettlementTargetCalculation> findBySettlementTargetId(UUID settlementTargetId);

	List<SettlementTargetCalculation> findBySettlementMonthAndSellerId(String settlementMonth, UUID sellerId);

	List<SettlementTargetSummary> findSummaryBySettlementMonth(String settlementMonth);
}
