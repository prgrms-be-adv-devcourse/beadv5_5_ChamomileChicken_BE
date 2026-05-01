package jabaclass.settlement.infrastructure.batch.dto;

import jabaclass.settlement.application.dto.AppliedPromotion;
import jabaclass.settlement.application.dto.SettlementTargetInfo;

public record PaymentTargetCalculationItem(
	SettlementTargetInfo target,
	AppliedPromotion appliedPromotion
) {
}
