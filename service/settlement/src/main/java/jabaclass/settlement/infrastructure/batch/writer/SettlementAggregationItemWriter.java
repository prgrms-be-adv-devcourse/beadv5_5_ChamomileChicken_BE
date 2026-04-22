package jabaclass.settlement.infrastructure.batch.writer;

import java.util.List;

import org.springframework.batch.infrastructure.item.Chunk;
import org.springframework.batch.infrastructure.item.ItemWriter;

import jabaclass.settlement.application.dto.SettlementTargetSummary;
import jabaclass.settlement.application.service.calculation.SettlementCalculateService;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class SettlementAggregationItemWriter implements ItemWriter<SettlementTargetSummary> {

	private final SettlementCalculateService settlementCalculateService;
	private final String settlementMonth;

	@Override
	public void write(Chunk<? extends SettlementTargetSummary> items) {
		List<SettlementTargetSummary> summaries = items.getItems().stream()
			.map(SettlementTargetSummary.class::cast)
			.toList();

		if (summaries.isEmpty()) {
			return;
		}

		settlementCalculateService.createAndSaveMonthlySettlements(summaries, settlementMonth);
	}
}
