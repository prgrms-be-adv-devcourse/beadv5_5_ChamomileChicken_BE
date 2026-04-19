package jabaclass.settlement.infrastructure.batch;

import jabaclass.settlement.domain.model.settlement.SettlementTarget;
import jabaclass.settlement.domain.model.settlement.SettlementTargetCalculation;

public record SettlementTargetCalculationBatchItem(
	SettlementTarget target,
	SettlementTargetCalculation calculation
) {
}
