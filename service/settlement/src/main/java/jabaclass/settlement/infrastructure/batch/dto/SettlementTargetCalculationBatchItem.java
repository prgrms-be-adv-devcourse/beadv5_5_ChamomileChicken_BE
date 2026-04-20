package jabaclass.settlement.infrastructure.batch.dto;

import jabaclass.settlement.domain.model.settlement.SettlementTarget;
import jabaclass.settlement.domain.model.settlement.SettlementTargetCalculation;

public record SettlementTargetCalculationBatchItem(
	SettlementTarget target,
	SettlementTargetCalculation calculation
) {
}
