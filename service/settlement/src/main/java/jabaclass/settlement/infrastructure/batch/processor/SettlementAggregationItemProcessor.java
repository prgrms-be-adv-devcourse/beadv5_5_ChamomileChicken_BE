package jabaclass.settlement.infrastructure.batch.processor;

import java.util.List;

import org.springframework.batch.infrastructure.item.ItemProcessor;

import jabaclass.settlement.application.dto.MonthlySettlementAggregationItem;
import jabaclass.settlement.application.dto.MonthlySettlementAggregationResult;
import jabaclass.settlement.application.service.calculation.SettlementCalculateService;
import jabaclass.settlement.domain.model.grade.SellerGradePolicy;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class SettlementAggregationItemProcessor implements
	ItemProcessor<MonthlySettlementAggregationItem, MonthlySettlementAggregationResult> {

	private final SettlementCalculateService settlementCalculateService;
	private final List<SellerGradePolicy> activeSellerGradePolicies;

	@Override
	public MonthlySettlementAggregationResult process(MonthlySettlementAggregationItem item) {
		return settlementCalculateService.aggregateMonthlySettlement(item, activeSellerGradePolicies);
	}
}
