package jabaclass.settlement.infrastructure.persistence;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jabaclass.settlement.domain.model.SettlementTarget;
import jabaclass.settlement.domain.model.SettlementTargetCalculationStatus;
import jabaclass.settlement.domain.model.SettlementTargetType;

public interface SettlementTargetJpaRepository extends JpaRepository<SettlementTarget, UUID> {

	boolean existsByPaymentId(UUID paymentId);

	boolean existsByRefundId(UUID refundId);

	Optional<SettlementTarget> findByPaymentIdAndTargetType(UUID paymentId, SettlementTargetType targetType);

	List<SettlementTarget> findBySettlementMonth(String settlementMonth);

	List<SettlementTarget> findBySettlementMonthAndSellerId(String settlementMonth, UUID sellerId);

	List<SettlementTarget> findByIdIn(List<UUID> ids);

	List<SettlementTarget> findBySettlementMonthAndCalculationStatus(
		String settlementMonth,
		SettlementTargetCalculationStatus calculationStatus
	);

	@Query("""
		select coalesce(sum(st.grossAmount), 0)
		from SettlementTarget st
		where st.sellerId = :sellerId
		  and st.settlementMonth in :settlementMonths
		""")
	BigDecimal sumGrossAmountBySellerIdAndSettlementMonths(
		@Param("sellerId") UUID sellerId,
		@Param("settlementMonths") List<String> settlementMonths
	);
}
