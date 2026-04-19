package jabaclass.settlement.infrastructure.batch;

import java.util.List;

import jabaclass.settlement.domain.model.settlement.Settlement;
import jabaclass.settlement.domain.model.settlement.SettlementTargetCalculation;

public record MonthlySettlementBatchItem(
	Settlement settlement,
	List<SettlementTargetCalculation> calculations
) {
}
