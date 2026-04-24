package jabaclass.settlement.infrastructure.batch.writer;

import java.util.List;

import org.springframework.batch.infrastructure.item.Chunk;
import org.springframework.batch.infrastructure.item.ItemWriter;

import jabaclass.settlement.application.service.SettlementTransferService;
import jabaclass.settlement.domain.model.settlement.Settlement;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class SettlementTransferItemWriter implements ItemWriter<Settlement> {

	private final SettlementTransferService settlementTransferService;

	@Override
	public void write(Chunk<? extends Settlement> items) {
		List<Settlement> settlements = items.getItems().stream()
			.map(Settlement.class::cast)
			.toList();

		if (settlements.isEmpty()) {
			return;
		}

		settlementTransferService.transferSettlements(settlements);
	}
}
