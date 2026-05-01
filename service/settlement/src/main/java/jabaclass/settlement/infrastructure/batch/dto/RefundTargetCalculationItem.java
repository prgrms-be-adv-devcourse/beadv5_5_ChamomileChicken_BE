package jabaclass.settlement.infrastructure.batch.dto;

import jabaclass.settlement.application.dto.AppliedPromotion;
import jabaclass.settlement.application.dto.SettlementTargetInfo;
import jabaclass.settlement.domain.model.settlement.SettlementTargetCalculation;

public record RefundTargetCalculationItem(
	SettlementTargetInfo target,
	SettlementTargetInfo originalPaymentTarget,
	SettlementTargetCalculation originalPaymentCalculation,
	AppliedPromotion fallbackAppliedPromotion
) {
}
