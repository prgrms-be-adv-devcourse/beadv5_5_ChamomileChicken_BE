package jabaclass.settlement.infrastructure.persistence;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import jabaclass.settlement.domain.model.SettlementBatchCursor;

public interface SettlementBatchCursorJpaRepository extends JpaRepository<SettlementBatchCursor, UUID> {

	Optional<SettlementBatchCursor> findByCursorType(String cursorType);
}