package jabaclass.settlement.domain.repository;

import java.util.Optional;

import jabaclass.settlement.domain.model.SettlementBatchCursor;

public interface SettlementBatchCursorRepository {

	SettlementBatchCursor save(SettlementBatchCursor cursor);

	Optional<SettlementBatchCursor> findByCursorType(String cursorType);
}