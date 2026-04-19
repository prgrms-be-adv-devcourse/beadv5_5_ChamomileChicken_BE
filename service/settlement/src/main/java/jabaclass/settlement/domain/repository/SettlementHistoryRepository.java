package jabaclass.settlement.domain.repository;

import java.util.List;
import java.util.UUID;

import jabaclass.settlement.domain.model.settlement.SettlementHistory;

public interface SettlementHistoryRepository {

	List<SettlementHistory> saveAll(List<SettlementHistory> settlementHistories);
}
