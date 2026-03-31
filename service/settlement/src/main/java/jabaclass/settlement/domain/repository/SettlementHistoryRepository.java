package jabaclass.settlement.domain.repository;

import java.util.List;
import java.util.UUID;

import jabaclass.settlement.domain.model.SettlementHistory;

public interface SettlementHistoryRepository {

	SettlementHistory save(SettlementHistory settlementHistory);

	List<SettlementHistory> saveAll(List<SettlementHistory> settlementHistories);

	List<SettlementHistory> findBySettlementId(UUID settlementId);

	List<SettlementHistory> findBySellerIdAndSettlementMonth(UUID sellerId, String settlementMonth);
}