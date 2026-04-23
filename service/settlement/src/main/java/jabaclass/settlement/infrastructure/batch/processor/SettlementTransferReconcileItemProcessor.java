package jabaclass.settlement.infrastructure.batch.processor;

import org.springframework.batch.infrastructure.item.ItemProcessor;
import org.springframework.stereotype.Component;

import jabaclass.settlement.domain.model.settlement.Settlement;
import jabaclass.settlement.domain.model.settlement.SettlementStatus;

@Component
public class SettlementTransferReconcileItemProcessor implements ItemProcessor<Settlement, Settlement> {

	@Override
	public Settlement process(Settlement settlement) {
		if (settlement.getStatus() != SettlementStatus.TRANSFERRING) {
			return null;
		}

		return settlement;
	}
}
