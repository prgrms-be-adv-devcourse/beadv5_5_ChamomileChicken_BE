package jabaclass.settlement.domain.repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import jabaclass.settlement.application.dto.SellerSalesAmount;
import jabaclass.settlement.application.dto.SettlementFeeRateAmount;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import jabaclass.settlement.domain.model.settlement.SettlementTargetCalculation;

public interface SettlementTargetCalculationRepository {

	List<SettlementTargetCalculation> saveAll(List<SettlementTargetCalculation> settlementTargetCalculations);

	List<SettlementTargetCalculation> findBySettlementTargetIds(List<UUID> settlementTargetIds);

	List<SellerSalesAmount> sumSettlementBaseAmountBySettlementMonths(List<String> settlementMonths);

	List<SettlementFeeRateAmount> sumSettlementBaseAmountBySettlementMonthGroupedBySellerAndFeeRate(String settlementMonth);

	Page<SettlementTargetCalculation> findBySettlementMonthAndSellerId(String settlementMonth, UUID sellerId, Pageable pageable);
}
