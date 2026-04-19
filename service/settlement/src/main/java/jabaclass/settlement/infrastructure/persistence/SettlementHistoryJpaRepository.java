package jabaclass.settlement.infrastructure.persistence;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import jabaclass.settlement.domain.model.SettlementHistory;

public interface SettlementHistoryJpaRepository extends JpaRepository<SettlementHistory, UUID> {
}
