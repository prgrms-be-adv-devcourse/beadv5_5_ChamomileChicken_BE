package jabaclass.settlement.infrastructure.persistence;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jabaclass.settlement.domain.model.settlement.SettlementTarget;
import jabaclass.settlement.domain.model.settlement.SettlementTargetCalculationStatus;

public interface SettlementTargetJpaRepository extends JpaRepository<SettlementTarget, UUID> {

	java.util.Optional<SettlementTarget> findByPaymentIdAndTargetType(
		UUID paymentId,
		jabaclass.settlement.domain.model.settlement.SettlementTargetType targetType
	);

	List<SettlementTarget> findByPaymentIdInAndTargetType(
		List<UUID> paymentIds,
		jabaclass.settlement.domain.model.settlement.SettlementTargetType targetType
	);

	java.util.Optional<SettlementTarget> findByRefundId(UUID refundId);

	List<SettlementTarget> findByIdIn(List<UUID> ids);

	List<SettlementTarget> findBySettlementMonthAndCalculationStatus(
		String settlementMonth,
		SettlementTargetCalculationStatus calculationStatus
	);

	@Query("""
		select
			st.sellerId as sellerId,
			coalesce(sum(st.settlementBaseAmount), 0) as salesAmount
		from SettlementTarget st
		where st.sellerId in :sellerIds
		  and st.settlementMonth in :settlementMonths
		group by st.sellerId
		""")
	List<SellerSalesAmountProjection> sumSettlementBaseAmountBySellerIdsAndSettlementMonths(
		@Param("sellerIds") List<UUID> sellerIds,
		@Param("settlementMonths") List<String> settlementMonths
	);

	interface SellerSalesAmountProjection {
		UUID getSellerId();
		BigDecimal getSalesAmount();
	}
}
