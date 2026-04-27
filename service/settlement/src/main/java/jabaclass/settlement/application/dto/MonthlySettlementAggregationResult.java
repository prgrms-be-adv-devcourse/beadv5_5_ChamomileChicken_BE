package jabaclass.settlement.application.dto;

import jabaclass.settlement.domain.model.grade.SellerGrade;
import jabaclass.settlement.domain.model.settlement.Settlement;

public record MonthlySettlementAggregationResult(
	Settlement settlement,
	SellerGrade sellerGrade
) {
}
