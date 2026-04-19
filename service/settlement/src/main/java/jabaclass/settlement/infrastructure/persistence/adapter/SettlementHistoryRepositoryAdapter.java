package jabaclass.settlement.infrastructure.persistence.adapter;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import jabaclass.settlement.domain.model.settlement.SettlementHistory;
import jabaclass.settlement.domain.repository.SettlementHistoryRepository;
import jabaclass.settlement.infrastructure.persistence.SettlementHistoryJpaRepository;
import lombok.RequiredArgsConstructor;


@Repository
@RequiredArgsConstructor
public class SettlementHistoryRepositoryAdapter implements SettlementHistoryRepository {

	private final SettlementHistoryJpaRepository settlementHistoryJpaRepository;

	@Override
	public List<SettlementHistory> saveAll(List<SettlementHistory> settlementHistories) {
		return settlementHistoryJpaRepository.saveAll(settlementHistories);
	}
}
