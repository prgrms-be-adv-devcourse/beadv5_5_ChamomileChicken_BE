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

	private final ItemStreamReader<UUID> delegate;
	private final SettlementTargetCalculationRepository settlementTargetCalculationRepository;
	private final SellerGradeRepository sellerGradeRepository;
	private final String settlementMonth;
	private final int chunkSize;

	private final Queue<SellerGradeCalculationItem> buffer = new ArrayDeque<>();
	private Map<UUID, java.math.BigDecimal> currentMonthSalesAmountBySellerId = Map.of();
	private Map<UUID, java.math.BigDecimal> recentThreeMonthSalesAmountBySellerId = Map.of();
	private Map<UUID, SellerGrade> sellerGradeBySellerId = Map.of();

	@Override
	public SellerGradeCalculationItem read() throws Exception {
		if (!buffer.isEmpty()) {
			return buffer.poll();
		}

		List<UUID> sellerIds = new ArrayList<>(chunkSize);
		while (sellerIds.size() < chunkSize) {
			UUID sellerId = delegate.read();
			if (sellerId == null) {
				break;
			}
			sellerIds.add(sellerId);
		}

		if (sellerIds.isEmpty()) {
			return null;
		}

		buffer.addAll(
			sellerIds.stream()
				.map(sellerId -> new SellerGradeCalculationItem(
					new SettlementTargetSummary(
						sellerId,
						settlementMonth,
						currentMonthSalesAmountBySellerId.getOrDefault(sellerId, java.math.BigDecimal.ZERO)
					),
					recentThreeMonthSalesAmountBySellerId.getOrDefault(sellerId, java.math.BigDecimal.ZERO),
					sellerGradeBySellerId.get(sellerId)
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
		this.currentMonthSalesAmountBySellerId = settlementTargetCalculationRepository.sumSettlementBaseAmountBySettlementMonths(
				List.of(settlementMonth)
			).stream()
			.collect(Collectors.toMap(
				SellerSalesAmount::sellerId,
				SellerSalesAmount::salesAmount,
				java.math.BigDecimal::add
			));
		this.recentThreeMonthSalesAmountBySellerId = settlementTargetCalculationRepository.sumSettlementBaseAmountBySettlementMonths(
				recentThreeMonths()
			).stream()
			.collect(Collectors.toMap(
				SellerSalesAmount::sellerId,
				SellerSalesAmount::salesAmount,
				java.math.BigDecimal::add
			));
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
		currentMonthSalesAmountBySellerId = Map.of();
		recentThreeMonthSalesAmountBySellerId = Map.of();
		sellerGradeBySellerId = Map.of();
	}
}
