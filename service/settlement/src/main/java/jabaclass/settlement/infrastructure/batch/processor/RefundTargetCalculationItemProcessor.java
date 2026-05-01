package jabaclass.settlement.infrastructure.batch.processor;

import org.springframework.batch.infrastructure.item.ItemProcessor;
import org.springframework.stereotype.Component;

import jabaclass.settlement.application.service.calculation.SettlementCalculateService;
import jabaclass.settlement.domain.model.settlement.SettlementTargetCalculation;
import jabaclass.settlement.infrastructure.batch.dto.RefundTargetCalculationItem;
import jabaclass.settlement.infrastructure.batch.dto.SettlementTargetCalculationBatchItem;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class RefundTargetCalculationItemProcessor
	implements ItemProcessor<RefundTargetCalculationItem, SettlementTargetCalculationBatchItem> {

	private final SettlementCalculateService settlementCalculateService;

	@Override
	public SettlementTargetCalculationBatchItem process(RefundTargetCalculationItem item) {
		SettlementTargetCalculation calculation = settlementCalculateService.calculateRefundTarget(
			item.target(),
			item.originalPaymentTarget(),
			item.originalPaymentCalculation(),
			item.fallbackAppliedPromotion()
		);
		return SettlementTargetCalculationBatchItem.calculated(item.target(), calculation);
	}
}
