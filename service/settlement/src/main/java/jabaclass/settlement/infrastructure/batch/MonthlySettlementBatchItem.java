package jabaclass.settlement.infrastructure.batch;

import java.util.List;

import jabaclass.settlement.domain.model.Settlement;
import jabaclass.settlement.domain.model.SettlementHistory;

public record MonthlySettlementBatchItem(
	Settlement settlement,
	List<SettlementHistory> histories
) {
}
