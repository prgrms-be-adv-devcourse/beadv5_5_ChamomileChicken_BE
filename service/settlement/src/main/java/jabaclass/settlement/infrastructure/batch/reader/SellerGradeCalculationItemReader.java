package jabaclass.settlement.infrastructure.batch.reader;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.batch.infrastructure.item.ExecutionContext;
import org.springframework.batch.infrastructure.item.ItemStreamException;
import org.springframework.batch.infrastructure.item.ItemStreamReader;

import jabaclass.settlement.application.dto.SellerGradeCalculationItem;
import jabaclass.settlement.application.dto.SellerSalesAmount;
import jabaclass.settlement.application.dto.SettlementTargetSummary;
import jabaclass.settlement.domain.model.grade.SellerGrade;
import jabaclass.settlement.domain.repository.SellerGradeRepository;
import jabaclass.settlement.domain.repository.SettlementTargetCalculationRepository;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class SellerGradeCalculationItemReader implements ItemStreamReader<SellerGradeCalculationItem> {

	private final ItemStreamReader<SettlementTargetSummary> delegate;
	private final SettlementTargetCalculationRepository settlementTargetCalculationRepository;
	private final SellerGradeRepository sellerGradeRepository;
	private final String settlementMonth;
	private final int chunkSize;

	private final Queue<SellerGradeCalculationItem> buffer = new ArrayDeque<>();

	@Override
	public SellerGradeCalculationItem read() throws Exception {
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

		List<UUID> sellerIds = summaries.stream()
			.map(SettlementTargetSummary::sellerId)
			.distinct()
			.toList();
		Map<UUID, java.math.BigDecimal> recentThreeMonthSalesAmountBySellerId =
			findRecentThreeMonthSalesAmountBySellerIds(sellerIds);
		Map<UUID, SellerGrade> sellerGradeBySellerId = sellerGradeRepository.findBySellerIds(sellerIds)
			.stream()
			.collect(Collectors.toMap(SellerGrade::getSellerId, Function.identity(), (existing, replacement) -> existing));

		buffer.addAll(
			summaries.stream()
				.map(summary -> new SellerGradeCalculationItem(
					summary,
					recentThreeMonthSalesAmountBySellerId.getOrDefault(summary.sellerId(), java.math.BigDecimal.ZERO),
					sellerGradeBySellerId.get(summary.sellerId())
				))
				.toList()
		);
		return buffer.poll();
	}

	private Map<UUID, java.math.BigDecimal> findRecentThreeMonthSalesAmountBySellerIds(List<UUID> sellerIds) {
		java.time.YearMonth baseMonth = java.time.YearMonth.parse(settlementMonth);
		List<String> recentThreeMonths = List.of(
			baseMonth.minusMonths(2).toString(),
			baseMonth.minusMonths(1).toString(),
			baseMonth.toString()
		);

		return settlementTargetCalculationRepository.sumSettlementBaseAmountBySellerIdsAndSettlementMonths(
				sellerIds,
				recentThreeMonths
			).stream()
			.collect(Collectors.toMap(
				SellerSalesAmount::sellerId,
				SellerSalesAmount::salesAmount,
				java.math.BigDecimal::add
			));
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
