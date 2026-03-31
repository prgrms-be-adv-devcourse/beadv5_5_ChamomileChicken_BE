package jabaclass.settlement.infrastructure.persistence;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import jabaclass.settlement.domain.model.SettlementTarget;

public interface SettlementTargetJpaRepository extends JpaRepository<SettlementTarget, UUID> {

	boolean existsByPaymentId(UUID paymentId);

	boolean existsByRefundId(UUID refundId);

	List<SettlementTarget> findBySettlementMonth(String settlementMonth);

	List<SettlementTarget> findBySettlementMonthAndSellerId(String settlementMonth, UUID sellerId);

	@Query("""
        select
            st.sellerId as sellerId,
            st.settlementMonth as settlementMonth,
            sum(st.settlementAmount) as totalSettlementAmount,
            count(st.id) as targetCount,
            count(distinct st.orderId) as orderCount
        from SettlementTarget st
        where st.settlementMonth = :settlementMonth
        group by st.sellerId, st.settlementMonth
        """)
	List<SettlementTargetSummaryProjection> findSummaryBySettlementMonth(String settlementMonth);

	interface SettlementTargetSummaryProjection {
		UUID getSellerId();
		String getSettlementMonth();
		java.math.BigDecimal getTotalSettlementAmount();
		Long getTargetCount();
		Long getOrderCount();
	}
}