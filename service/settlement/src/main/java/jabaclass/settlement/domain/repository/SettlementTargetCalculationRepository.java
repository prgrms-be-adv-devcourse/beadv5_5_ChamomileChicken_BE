package jabaclass.settlement.domain.repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import jabaclass.settlement.application.dto.SettlementTargetSummary;
import jabaclass.settlement.domain.model.settlement.SettlementTargetCalculation;

public interface SettlementTargetCalculationRepository {

	List<SettlementTargetCalculation> saveAll(List<SettlementTargetCalculation> settlementTargetCalculations);

	boolean existsBySettlementTargetId(UUID settlementTargetId);

	Optional<SettlementTargetCalculation> findBySettlementTargetId(UUID settlementTargetId);

	List<SettlementTargetCalculation> findBySettlementMonthAndSellerId(String settlementMonth, UUID sellerId);

	Page<SettlementTargetCalculation> findBySettlementMonthAndSellerId(String settlementMonth, UUID sellerId, Pageable pageable);

	List<SettlementTargetSummary> findSummaryBySettlementMonth(String settlementMonth);
}
