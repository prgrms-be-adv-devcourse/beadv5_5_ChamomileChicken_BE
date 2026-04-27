package jabaclass.settlement.infrastructure.batch.writer;

import java.util.List;
import java.util.Objects;

import org.springframework.batch.infrastructure.item.Chunk;
import org.springframework.batch.infrastructure.item.ItemWriter;
import org.springframework.stereotype.Component;

import jabaclass.settlement.domain.model.settlement.SettlementTarget;
import jabaclass.settlement.domain.model.settlement.SettlementTargetCalculation;
import jabaclass.settlement.domain.repository.SettlementTargetCalculationRepository;
import jabaclass.settlement.domain.repository.SettlementTargetRepository;
import jabaclass.settlement.infrastructure.batch.dto.SettlementTargetCalculationBatchItem;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class SettlementTargetCalculationItemWriter implements ItemWriter<SettlementTargetCalculationBatchItem> {

	private final SettlementTargetRepository settlementTargetRepository;
	private final SettlementTargetCalculationRepository settlementTargetCalculationRepository;

	@Override
	public void write(Chunk<? extends SettlementTargetCalculationBatchItem> items) {
		List<SettlementTarget> targets = items.getItems().stream()
			.map(SettlementTargetCalculationBatchItem::target)
			.toList();
		List<SettlementTargetCalculation> calculations = items.getItems().stream()
			.map(SettlementTargetCalculationBatchItem::calculation)
			.filter(Objects::nonNull)
			.toList();

		if (!calculations.isEmpty()) {
			settlementTargetCalculationRepository.saveAll(calculations);
		}
		if (!targets.isEmpty()) {
			settlementTargetRepository.saveAll(targets);
		}
	}
}
