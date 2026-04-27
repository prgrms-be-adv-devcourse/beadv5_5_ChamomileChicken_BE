package jabaclass.settlement.infrastructure.batch.reader;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;

import org.springframework.batch.infrastructure.item.ExecutionContext;
import org.springframework.batch.infrastructure.item.ItemStreamException;
import org.springframework.batch.infrastructure.item.ItemStreamReader;

import jabaclass.settlement.application.dto.MonthlySettlementAggregationItem;
import jabaclass.settlement.application.dto.SettlementTargetSummary;
import jabaclass.settlement.application.service.calculation.SettlementAggregationItemAssembler;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class SettlementAggregationItemReader implements ItemStreamReader<MonthlySettlementAggregationItem> {

	private final ItemStreamReader<SettlementTargetSummary> delegate;
	private final SettlementAggregationItemAssembler settlementAggregationItemAssembler;
	private final String settlementMonth;
	private final int chunkSize;

	private final Queue<MonthlySettlementAggregationItem> buffer = new ArrayDeque<>();

	@Override
	public MonthlySettlementAggregationItem read() throws Exception {
		if (!buffer.isEmpty()) {
			return buffer.poll();
		}

		List<SettlementTargetSummary> summaries = new ArrayList<>(chunkSize);
		while (summaries.size() < chunkSize) {
			SettlementTargetSummary summary = delegate.read();
			if (summary == null) {
				break;
			}
			summaries.add(summary);
		}

		if (summaries.isEmpty()) {
			return null;
		}

		buffer.addAll(settlementAggregationItemAssembler.assemble(summaries, settlementMonth));
		return buffer.poll();
	}

	@Override
	public void open(ExecutionContext executionContext) throws ItemStreamException {
		delegate.open(executionContext);
	}

	@Override
	public void update(ExecutionContext executionContext) throws ItemStreamException {
		delegate.update(executionContext);
	}

	@Override
	public void close() throws ItemStreamException {
		delegate.close();
	}
}
