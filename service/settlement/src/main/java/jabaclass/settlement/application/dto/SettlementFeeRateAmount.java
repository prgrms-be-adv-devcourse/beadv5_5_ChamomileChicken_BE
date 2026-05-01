package jabaclass.settlement.application.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record SettlementFeeRateAmount(
	UUID sellerId,
	BigDecimal appliedFeeRate,
	BigDecimal settlementBaseAmount
) {
}
