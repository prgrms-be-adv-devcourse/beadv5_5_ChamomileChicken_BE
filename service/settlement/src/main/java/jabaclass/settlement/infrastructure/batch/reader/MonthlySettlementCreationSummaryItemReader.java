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

import jabaclass.settlement.application.dto.MonthlySettlementCreationItem;
import jabaclass.settlement.application.dto.SellerSalesAmount;
import jabaclass.settlement.application.dto.SettlementFeeRateAmount;
import jabaclass.settlement.application.dto.SettlementTargetSummary;
import jabaclass.settlement.domain.model.grade.SellerGrade;
import jabaclass.settlement.domain.model.settlement.Settlement;
import jabaclass.settlement.domain.repository.SellerGradeRepository;
import jabaclass.settlement.domain.repository.SettlementRepository;
import jabaclass.settlement.domain.repository.SettlementTargetCalculationRepository;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class MonthlySettlementCreationSummaryItemReader implements ItemStreamReader<MonthlySettlementCreationItem> {

	private final ItemStreamReader<SettlementTargetSummary> delegate;
	private final SettlementRepository settlementRepository;
	private final SettlementTargetCalculationRepository settlementTargetCalculationRepository;
	private final SellerGradeRepository sellerGradeRepository;
	private final String settlementMonth;
	private final int chunkSize;

	private final Queue<MonthlySettlementCreationItem> buffer = new ArrayDeque<>();
	private Map<UUID, Settlement> existingSettlementBySellerId = Map.of();
	private Map<UUID, java.math.BigDecimal> recentThreeMonthSalesAmountBySellerId = Map.of();
	private Map<UUID, List<SettlementFeeRateAmount>> feeRateAmountsBySellerId = Map.of();
	private Map<UUID, SellerGrade> sellerGradeBySellerId = Map.of();

	@Override
	public MonthlySettlementCreationItem read() throws Exception {
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

		buffer.addAll(
			summaries.stream()
				.map(summary -> new MonthlySettlementCreationItem(
					summary,
					existingSettlementBySellerId.get(summary.sellerId()),
					recentThreeMonthSalesAmountBySellerId.getOrDefault(summary.sellerId(), java.math.BigDecimal.ZERO),
					feeRateAmountsBySellerId.getOrDefault(summary.sellerId(), List.of()),
					sellerGradeBySellerId.get(summary.sellerId())
				))
				.toList()
		);
		return buffer.poll();
	}

	private List<String> recentThreeMonths() {
		java.time.YearMonth baseMonth = java.time.YearMonth.parse(settlementMonth);
		return List.of(
			baseMonth.minusMonths(2).toString(),
			baseMonth.minusMonths(1).toString(),
			baseMonth.toString()
		);
	}

	@Override
	public void open(ExecutionContext executionContext) throws ItemStreamException {
		this.existingSettlementBySellerId = settlementRepository.findBySettlementMonth(settlementMonth)
			.stream()
			.collect(Collectors.toMap(Settlement::getSellerId, Function.identity(), (existing, replacement) -> existing));
		this.recentThreeMonthSalesAmountBySellerId = settlementTargetCalculationRepository.sumSettlementBaseAmountBySettlementMonths(
				recentThreeMonths()
			).stream()
			.collect(Collectors.toMap(
				SellerSalesAmount::sellerId,
				SellerSalesAmount::salesAmount,
				java.math.BigDecimal::add
			));
		this.feeRateAmountsBySellerId = settlementTargetCalculationRepository
			.sumSettlementBaseAmountBySettlementMonthGroupedBySellerAndFeeRate(settlementMonth)
			.stream()
			.collect(Collectors.groupingBy(SettlementFeeRateAmount::sellerId));
		this.sellerGradeBySellerId = sellerGradeRepository.findByCalculatedMonth(settlementMonth)
			.stream()
			.collect(Collectors.toMap(SellerGrade::getSellerId, Function.identity(), (existing, replacement) -> existing));
		delegate.open(executionContext);
	}

	@Override
	public void update(ExecutionContext executionContext) throws ItemStreamException {
		delegate.update(executionContext);
	}

	@Override
	public void close() throws ItemStreamException {
		delegate.close();
		buffer.clear();
		existingSettlementBySellerId = Map.of();
		recentThreeMonthSalesAmountBySellerId = Map.of();
		feeRateAmountsBySellerId = Map.of();
		sellerGradeBySellerId = Map.of();
	}
}
