package jabaclass.settlement.infrastructure.persistence;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import jabaclass.settlement.domain.model.SettlementHistory;

public interface SettlementHistoryJpaRepository extends JpaRepository<SettlementHistory, UUID> {

	List<SettlementHistory> findBySettlementId(UUID settlementId);

	List<SettlementHistory> findBySellerIdAndSettlementMonth(UUID sellerId, String settlementMonth);
}