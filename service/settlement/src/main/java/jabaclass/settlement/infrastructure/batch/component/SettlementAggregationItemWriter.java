package jabaclass.settlement.infrastructure.batch.component;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.springframework.batch.infrastructure.item.Chunk;
import org.springframework.batch.infrastructure.item.ItemWriter;
import org.springframework.stereotype.Component;

import jabaclass.settlement.application.service.calculation.SettlementCalculateService;
import jabaclass.settlement.domain.model.settlement.Settlement;
import jabaclass.settlement.domain.model.settlement.SettlementHistory;
import jabaclass.settlement.domain.repository.SettlementHistoryRepository;
import jabaclass.settlement.domain.repository.SettlementRepository;
import jabaclass.settlement.infrastructure.batch.dto.MonthlySettlementBatchItem;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class SettlementAggregationItemWriter implements ItemWriter<MonthlySettlementBatchItem> {

	private final SettlementRepository settlementRepository;
	private final SettlementHistoryRepository settlementHistoryRepository;
	private final SettlementCalculateService settlementCalculateService;

	@Override
	public void write(Chunk<? extends MonthlySettlementBatchItem> items) {
		List<MonthlySettlementBatchItem> validItems = items.getItems().stream()
			.filter(Objects::nonNull)
			.map(MonthlySettlementBatchItem.class::cast)
			.toList();

		if (validItems.isEmpty()) {
			return;
		}

		List<Settlement> savedSettlements = settlementRepository.saveAll(validItems.stream()
			.map(MonthlySettlementBatchItem::settlement)
			.toList());

		List<SettlementHistory> histories = new ArrayList<>();
		for (int i = 0; i < savedSettlements.size(); i++) {
			histories.addAll(settlementCalculateService.createHistories(
				savedSettlements.get(i),
				validItems.get(i).calculations()
			));
		}

		if (!histories.isEmpty()) {
			settlementHistoryRepository.saveAll(histories);
		}
	}
}
