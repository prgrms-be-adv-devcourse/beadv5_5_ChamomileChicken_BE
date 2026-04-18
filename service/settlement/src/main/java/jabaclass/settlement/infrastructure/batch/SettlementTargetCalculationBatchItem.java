package jabaclass.settlement.infrastructure.batch;

import jabaclass.settlement.domain.model.SettlementTarget;
import jabaclass.settlement.domain.model.SettlementTargetCalculation;

public record SettlementTargetCalculationBatchItem(
	SettlementTarget target,
	SettlementTargetCalculation calculation
) {
}
