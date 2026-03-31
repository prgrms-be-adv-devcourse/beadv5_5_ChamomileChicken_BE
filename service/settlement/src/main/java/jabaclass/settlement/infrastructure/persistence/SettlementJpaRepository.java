package jabaclass.settlement.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import jabaclass.settlement.domain.model.Settlement;
import jabaclass.settlement.domain.model.SettlementStatus;

public interface SettlementJpaRepository extends JpaRepository<Settlement, UUID> {

	Optional<Settlement> findBySellerIdAndSettlementMonth(UUID sellerId, String settlementMonth);

	boolean existsBySellerIdAndSettlementMonth(UUID sellerId, String settlementMonth);

	List<Settlement> findByStatus(SettlementStatus status);

	List<Settlement> findBySettlementMonth(String settlementMonth);

	List<Settlement> findBySettlementMonthAndStatus(String settlementMonth, SettlementStatus status);
}