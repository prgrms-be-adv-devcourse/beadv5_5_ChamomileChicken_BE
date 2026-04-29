package jabaclass.settlement.infrastructure.batch.dto;

import jabaclass.settlement.application.dto.AppliedPromotion;
import jabaclass.settlement.domain.model.settlement.SettlementTarget;
import jabaclass.settlement.domain.model.settlement.SettlementTargetCalculation;

public record RefundTargetCalculationItem(
	SettlementTarget target,
	SettlementTarget originalPaymentTarget,
	SettlementTargetCalculation originalPaymentCalculation,
	AppliedPromotion fallbackAppliedPromotion
) {
}
