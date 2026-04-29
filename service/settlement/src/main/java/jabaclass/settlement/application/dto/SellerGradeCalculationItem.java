package jabaclass.settlement.application.dto;

import java.math.BigDecimal;

import jabaclass.settlement.domain.model.grade.SellerGrade;

public record SellerGradeCalculationItem(
	SettlementTargetSummary summary,
	BigDecimal recentThreeMonthSalesAmount,
	SellerGrade sellerGrade
) {
}
