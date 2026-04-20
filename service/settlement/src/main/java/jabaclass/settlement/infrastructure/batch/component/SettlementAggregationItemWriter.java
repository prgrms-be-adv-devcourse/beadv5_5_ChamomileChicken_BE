package jabaclass.settlement.infrastructure.batch.component;

import java.util.List;
import java.util.Objects;

import org.springframework.batch.infrastructure.item.Chunk;
import org.springframework.batch.infrastructure.item.ItemWriter;
import org.springframework.stereotype.Component;

import jabaclass.settlement.domain.repository.SettlementRepository;
import jabaclass.settlement.infrastructure.batch.dto.MonthlySettlementBatchItem;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class SettlementAggregationItemWriter implements ItemWriter<MonthlySettlementBatchItem> {

	private final SettlementRepository settlementRepository;

	@Override
	public void write(Chunk<? extends MonthlySettlementBatchItem> items) {
		List<MonthlySettlementBatchItem> validItems = items.getItems().stream()
			.filter(Objects::nonNull)
			.map(MonthlySettlementBatchItem.class::cast)
			.toList();

		if (validItems.isEmpty()) {
			return;
		}

		settlementRepository.saveAll(validItems.stream()
			.map(MonthlySettlementBatchItem::settlement)
			.toList());
	}
}
