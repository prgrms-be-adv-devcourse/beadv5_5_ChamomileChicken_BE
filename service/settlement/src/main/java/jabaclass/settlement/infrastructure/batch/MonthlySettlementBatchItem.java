package jabaclass.settlement.infrastructure.batch;

import java.util.List;

import jabaclass.settlement.domain.model.Settlement;
import jabaclass.settlement.domain.model.SettlementTargetCalculation;

public record MonthlySettlementBatchItem(
	Settlement settlement,
	List<SettlementTargetCalculation> calculations
) {
}
