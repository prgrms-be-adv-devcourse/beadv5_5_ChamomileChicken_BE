package jabaclass.settlement.infrastructure.batch.processor;

import java.util.List;

import org.springframework.batch.infrastructure.item.ItemProcessor;

import jabaclass.settlement.application.dto.SellerGradeCalculationItem;
import jabaclass.settlement.application.service.calculation.SettlementCalculateService;
import jabaclass.settlement.domain.model.grade.SellerGrade;
import jabaclass.settlement.domain.model.grade.SellerGradePolicy;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class SellerGradeCalculationItemProcessor implements ItemProcessor<SellerGradeCalculationItem, SellerGrade> {

	private final SettlementCalculateService settlementCalculateService;
	private final List<SellerGradePolicy> activeSellerGradePolicies;

	@Override
	public SellerGrade process(SellerGradeCalculationItem item) {
		return settlementCalculateService.calculateSellerGrade(item, activeSellerGradePolicies);
	}
}
