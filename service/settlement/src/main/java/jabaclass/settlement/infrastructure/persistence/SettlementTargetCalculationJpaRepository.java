package jabaclass.settlement.infrastructure.persistence;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import jabaclass.settlement.domain.model.settlement.SettlementTargetCalculation;

public interface SettlementTargetCalculationJpaRepository extends JpaRepository<SettlementTargetCalculation, UUID> {

	boolean existsBySettlementTargetId(UUID settlementTargetId);

	Optional<SettlementTargetCalculation> findBySettlementTargetId(UUID settlementTargetId);

	List<SettlementTargetCalculation> findBySettlementMonthAndSellerId(String settlementMonth, UUID sellerId);

	List<SettlementTargetCalculation> findBySettlementMonthAndSellerIdIn(String settlementMonth, List<UUID> sellerIds);

	Page<SettlementTargetCalculation> findBySettlementMonthAndSellerId(String settlementMonth, UUID sellerId, Pageable pageable);

	@Query("""
		select
			stc.sellerId as sellerId,
			stc.settlementMonth as settlementMonth,
			sum(stc.settlementBaseAmount) as totalSettlementBaseAmount
		from SettlementTargetCalculation stc
		where stc.settlementMonth = :settlementMonth
		group by stc.sellerId, stc.settlementMonth
		order by stc.sellerId
		""")
	List<SettlementTargetSummaryProjection> findSummaryBySettlementMonth(String settlementMonth);

	interface SettlementTargetSummaryProjection {
		UUID getSellerId();
		String getSettlementMonth();
		BigDecimal getTotalSettlementBaseAmount();
	}
}
