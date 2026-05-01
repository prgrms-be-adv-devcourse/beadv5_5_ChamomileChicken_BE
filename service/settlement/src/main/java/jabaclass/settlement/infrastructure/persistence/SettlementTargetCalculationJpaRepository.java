package jabaclass.settlement.infrastructure.persistence;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jabaclass.settlement.domain.model.settlement.SettlementTargetCalculation;

public interface SettlementTargetCalculationJpaRepository extends JpaRepository<SettlementTargetCalculation, UUID> {

	List<SettlementTargetCalculation> findBySettlementTargetIdIn(List<UUID> settlementTargetIds);

	@Query("""
		select
			stc.sellerId as sellerId,
			coalesce(sum(stc.settlementBaseAmount), 0) as salesAmount
		from SettlementTargetCalculation stc
		where stc.settlementMonth in :settlementMonths
		group by stc.sellerId
		""")
	List<SellerSalesAmountProjection> sumSettlementBaseAmountBySettlementMonths(
		@Param("settlementMonths") List<String> settlementMonths
	);

	@Query("""
		select
			stc.sellerId as sellerId,
			stc.appliedFeeRate as appliedFeeRate,
			coalesce(sum(stc.settlementBaseAmount), 0) as settlementBaseAmount
		from SettlementTargetCalculation stc
		where stc.settlementMonth = :settlementMonth
		group by stc.sellerId, stc.appliedFeeRate
		""")
	List<SellerFeeRateAmountProjection> sumSettlementBaseAmountBySettlementMonthGroupedBySellerAndFeeRate(
		@Param("settlementMonth") String settlementMonth
	);

	Page<SettlementTargetCalculation> findBySettlementMonthAndSellerId(String settlementMonth, UUID sellerId, Pageable pageable);

	interface SellerSalesAmountProjection {
		UUID getSellerId();
		BigDecimal getSalesAmount();
	}

	interface SellerFeeRateAmountProjection {
		UUID getSellerId();
		BigDecimal getAppliedFeeRate();
		BigDecimal getSettlementBaseAmount();
	}
}
