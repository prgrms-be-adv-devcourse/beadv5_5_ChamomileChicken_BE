package jabaclass.settlement.infrastructure.persistence.adapter;

import java.util.Optional;

import org.springframework.stereotype.Repository;

import jabaclass.settlement.domain.model.SettlementBatchCursor;
import jabaclass.settlement.domain.repository.SettlementBatchCursorRepository;
import jabaclass.settlement.infrastructure.persistence.SettlementBatchCursorJpaRepository;
import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class SettlementBatchCursorRepositoryAdapter implements SettlementBatchCursorRepository {

	private final SettlementBatchCursorJpaRepository settlementBatchCursorJpaRepository;

	@Override
	public SettlementBatchCursor save(SettlementBatchCursor cursor) {
		return settlementBatchCursorJpaRepository.save(cursor);
	}

	@Override
	public Optional<SettlementBatchCursor> findByCursorType(String cursorType) {
		return settlementBatchCursorJpaRepository.findByCursorType(cursorType);
	}
}