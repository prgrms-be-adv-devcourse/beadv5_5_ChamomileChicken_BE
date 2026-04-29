package jabaclass.settlement.infrastructure.batch.processor;

import java.util.List;

import org.springframework.batch.infrastructure.item.ItemProcessor;

import jabaclass.settlement.application.dto.MonthlySettlementCreationItem;
import jabaclass.settlement.application.service.calculation.SettlementCalculateService;
import jabaclass.settlement.domain.model.grade.SellerGradePolicy;
import jabaclass.settlement.domain.model.settlement.Settlement;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class MonthlySettlementCreationItemProcessor implements ItemProcessor<MonthlySettlementCreationItem, Settlement> {

	private final SettlementCalculateService settlementCalculateService;
	private final List<SellerGradePolicy> activeSellerGradePolicies;

	@Override
	public Settlement process(MonthlySettlementCreationItem item) {
		return settlementCalculateService.createMonthlySettlement(item, activeSellerGradePolicies);
	}
}
