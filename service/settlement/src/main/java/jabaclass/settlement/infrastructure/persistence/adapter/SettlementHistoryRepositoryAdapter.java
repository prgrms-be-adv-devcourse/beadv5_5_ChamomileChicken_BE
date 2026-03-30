package jabaclass.settlement.infrastructure.persistence.adapter;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import jabaclass.settlement.domain.model.SettlementHistory;
import jabaclass.settlement.domain.repository.SettlementHistoryRepository;
import jabaclass.settlement.infrastructure.persistence.SettlementHistoryJpaRepository;
import lombok.RequiredArgsConstructor;


@Repository
@RequiredArgsConstructor
public class SettlementHistoryRepositoryAdapter implements SettlementHistoryRepository {

	private final SettlementHistoryJpaRepository settlementHistoryJpaRepository;

	@Override
	public SettlementHistory save(SettlementHistory settlementHistory) {
		return settlementHistoryJpaRepository.save(settlementHistory);
	}

	@Override
	public List<SettlementHistory> saveAll(List<SettlementHistory> settlementHistories) {
		return settlementHistoryJpaRepository.saveAll(settlementHistories);
	}

	@Override
	public List<SettlementHistory> findBySettlementId(UUID settlementId) {
		return settlementHistoryJpaRepository.findBySettlementId(settlementId);
	}

	@Override
	public List<SettlementHistory> findBySellerIdAndSettlementMonth(UUID sellerId, String settlementMonth) {
		return settlementHistoryJpaRepository.findBySellerIdAndSettlementMonth(sellerId, settlementMonth);
	}
}