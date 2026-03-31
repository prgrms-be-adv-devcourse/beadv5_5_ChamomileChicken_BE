package jabaclass.settlement.application.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record SettlementTargetSummary(
	UUID sellerId,
	String settlementMonth,
	BigDecimal totalSettlementAmount,
	Long targetCount,
	Long orderCount
) {
}
