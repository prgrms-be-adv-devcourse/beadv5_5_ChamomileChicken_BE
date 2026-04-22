package jabaclass.settlement.infrastructure.batch.writer;

import java.util.List;
import java.util.Objects;

import org.springframework.batch.infrastructure.item.Chunk;
import org.springframework.batch.infrastructure.item.ItemWriter;
import org.springframework.stereotype.Component;

import jabaclass.settlement.domain.model.settlement.Settlement;
import jabaclass.settlement.domain.repository.SettlementRepository;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class SettlementAggregationItemWriter implements ItemWriter<Settlement> {

	private final SettlementRepository settlementRepository;

	@Override
	public void write(Chunk<? extends Settlement> items) {
		List<Settlement> settlements = items.getItems().stream()
			.filter(Objects::nonNull)
			.map(Settlement.class::cast)
			.toList();

		if (settlements.isEmpty()) {
			return;
		}

		settlementRepository.saveAll(settlements);
	}
}
