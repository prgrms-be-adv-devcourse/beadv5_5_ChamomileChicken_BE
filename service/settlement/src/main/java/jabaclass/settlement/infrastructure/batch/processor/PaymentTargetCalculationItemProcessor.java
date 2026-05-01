package jabaclass.settlement.infrastructure.batch.processor;

import org.springframework.batch.infrastructure.item.ItemProcessor;
import org.springframework.stereotype.Component;

import jabaclass.settlement.application.exception.SettlementCalculationRetryableException;
import jabaclass.settlement.application.service.calculation.SettlementCalculateService;
import jabaclass.settlement.domain.model.settlement.SettlementTargetCalculation;
import jabaclass.settlement.infrastructure.batch.dto.PaymentTargetCalculationItem;
import jabaclass.settlement.infrastructure.batch.dto.SettlementTargetCalculationBatchItem;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class PaymentTargetCalculationItemProcessor
	implements ItemProcessor<PaymentTargetCalculationItem, SettlementTargetCalculationBatchItem> {

	private final SettlementCalculateService settlementCalculateService;

	@Override
	public SettlementTargetCalculationBatchItem process(PaymentTargetCalculationItem item) {
		try {
			SettlementTargetCalculation calculation = settlementCalculateService.calculatePaymentTarget(
				item.target(),
				item.appliedPromotion()
			);
			return SettlementTargetCalculationBatchItem.calculated(item.target(), calculation);
		} catch (SettlementCalculationRetryableException e) {
			return null;
		} catch (Exception e) {
			return SettlementTargetCalculationBatchItem.failed(item.target(), e.getMessage());
		}
	}
}
