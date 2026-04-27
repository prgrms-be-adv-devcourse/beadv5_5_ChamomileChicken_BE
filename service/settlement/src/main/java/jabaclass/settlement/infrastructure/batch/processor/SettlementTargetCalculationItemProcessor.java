package jabaclass.settlement.infrastructure.batch.processor;

import org.springframework.batch.infrastructure.item.ItemProcessor;
import org.springframework.stereotype.Component;

import jabaclass.settlement.application.exception.SettlementCalculationRetryableException;
import jabaclass.settlement.application.service.calculation.SettlementCalculateService;
import jabaclass.settlement.domain.model.settlement.SettlementTarget;
import jabaclass.settlement.domain.model.settlement.SettlementTargetCalculation;
import jabaclass.settlement.domain.model.settlement.SettlementTargetCalculationStatus;
import jabaclass.settlement.infrastructure.batch.dto.SettlementTargetCalculationBatchItem;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class SettlementTargetCalculationItemProcessor
	implements ItemProcessor<SettlementTarget, SettlementTargetCalculationBatchItem> {

	private final SettlementCalculateService settlementCalculateService;

	@Override
	public SettlementTargetCalculationBatchItem process(SettlementTarget target) {
		if (target.getCalculationStatus() != SettlementTargetCalculationStatus.PENDING) {
			return null;
		}

		try {
			SettlementTargetCalculation calculation = settlementCalculateService.calculateTarget(target);
			settlementCalculateService.markTargetCalculated(target);
			return new SettlementTargetCalculationBatchItem(target, calculation);
		} catch (SettlementCalculationRetryableException e) {
			return null;
		} catch (Exception e) {
			settlementCalculateService.markTargetCalculationFailed(target, e);
			return new SettlementTargetCalculationBatchItem(target, null);
		}
	}
}
