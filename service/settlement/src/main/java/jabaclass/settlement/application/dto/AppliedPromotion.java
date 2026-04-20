package jabaclass.settlement.application.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record AppliedPromotion(
	UUID promotionId,
	String promotionType,
	BigDecimal feeRate
) {
	public static AppliedPromotion empty() {
		return new AppliedPromotion(null, null, null);
	}

	public boolean exists() {
		return promotionId != null;
	}
}
