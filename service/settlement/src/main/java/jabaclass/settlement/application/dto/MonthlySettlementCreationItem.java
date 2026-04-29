package jabaclass.settlement.application.dto;

import java.math.BigDecimal;
import java.util.List;

import jabaclass.settlement.domain.model.grade.SellerGrade;
import jabaclass.settlement.domain.model.settlement.Settlement;
import jabaclass.settlement.domain.model.settlement.SettlementTargetCalculation;

public record MonthlySettlementCreationItem(
	SettlementTargetSummary summary,
	Settlement existingSettlement,
	BigDecimal recentThreeMonthSalesAmount,
	List<SettlementTargetCalculation> calculations,
	SellerGrade sellerGrade
) {
}
