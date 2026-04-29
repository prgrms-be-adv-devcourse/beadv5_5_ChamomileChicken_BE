package jabaclass.settlement.infrastructure.batch.dto;

import jabaclass.settlement.application.dto.AppliedPromotion;
import jabaclass.settlement.domain.model.settlement.SettlementTarget;

public record PaymentTargetCalculationItem(
	SettlementTarget target,
	AppliedPromotion appliedPromotion
) {
}
