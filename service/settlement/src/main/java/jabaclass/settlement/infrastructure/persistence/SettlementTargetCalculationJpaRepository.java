package jabaclass.settlement.infrastructure.persistence;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jabaclass.settlement.domain.model.SettlementTargetCalculation;

public interface SettlementTargetCalculationJpaRepository extends JpaRepository<SettlementTargetCalculation, UUID> {

	boolean existsBySettlementTargetId(UUID settlementTargetId);

	Optional<SettlementTargetCalculation> findBySettlementTargetId(UUID settlementTargetId);

	List<SettlementTargetCalculation> findBySettlementMonth(String settlementMonth);

	List<SettlementTargetCalculation> findBySettlementMonthAndSellerId(String settlementMonth, UUID sellerId);

	@Query("""
		select coalesce(sum(stc.settlementBaseAmount), 0)
		from SettlementTargetCalculation stc
		where stc.sellerId = :sellerId
		  and stc.settlementMonth in :settlementMonths
		""")
	BigDecimal sumSettlementBaseAmountBySellerIdAndSettlementMonths(
		@Param("sellerId") UUID sellerId,
		@Param("settlementMonths") List<String> settlementMonths
	);

	@Query("""
		select
			stc.sellerId as sellerId,
			stc.settlementMonth as settlementMonth,
			sum(stc.settlementBaseAmount) as totalSettlementBaseAmount,
			sum(stc.feeAmount) as totalFeeAmount,
			sum(stc.settlementAmount) as totalSettlementAmount,
			count(stc.id) as targetCount
		from SettlementTargetCalculation stc
		where stc.settlementMonth = :settlementMonth
		group by stc.sellerId, stc.settlementMonth
		""")
	List<SettlementTargetSummaryProjection> findSummaryBySettlementMonth(String settlementMonth);

	interface SettlementTargetSummaryProjection {
		UUID getSellerId();
		String getSettlementMonth();
		BigDecimal getTotalSettlementBaseAmount();
		BigDecimal getTotalFeeAmount();
		BigDecimal getTotalSettlementAmount();
		Long getTargetCount();
	}
}
