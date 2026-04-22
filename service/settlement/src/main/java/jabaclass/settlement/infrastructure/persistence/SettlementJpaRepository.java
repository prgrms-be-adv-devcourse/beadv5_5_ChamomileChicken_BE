package jabaclass.settlement.infrastructure.persistence;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import jabaclass.settlement.domain.model.settlement.Settlement;
import jabaclass.settlement.domain.model.settlement.SettlementStatus;

public interface SettlementJpaRepository extends JpaRepository<Settlement, UUID> {

	boolean existsBySellerIdAndSettlementMonth(UUID sellerId, String settlementMonth);

	List<Settlement> findBySettlementMonthAndSellerIdIn(String settlementMonth, List<UUID> sellerIds);

	Page<Settlement> findBySellerId(UUID sellerId, Pageable pageable);

	List<Settlement> findBySettlementMonth(String settlementMonth);

	List<Settlement> findBySettlementMonthAndStatus(String settlementMonth, SettlementStatus status);
}
