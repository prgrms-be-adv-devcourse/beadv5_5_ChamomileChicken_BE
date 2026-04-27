package jabaclass.settlement.application.service.calculation;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import jabaclass.settlement.application.dto.MonthlySettlementAggregationItem;
import jabaclass.settlement.application.dto.SellerSalesAmount;
import jabaclass.settlement.application.dto.SettlementTargetSummary;
import jabaclass.settlement.domain.model.grade.SellerGrade;
import jabaclass.settlement.domain.model.settlement.Settlement;
import jabaclass.settlement.domain.model.settlement.SettlementTargetCalculation;
import jabaclass.settlement.domain.repository.SellerGradeRepository;
import jabaclass.settlement.domain.repository.SettlementRepository;
import jabaclass.settlement.domain.repository.SettlementTargetCalculationRepository;
import jabaclass.settlement.domain.repository.SettlementTargetRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SettlementAggregationItemAssembler {

	private final SettlementRepository settlementRepository;
	private final SettlementTargetRepository settlementTargetRepository;
	private final SettlementTargetCalculationRepository settlementTargetCalculationRepository;
	private final SellerGradeRepository sellerGradeRepository;

	public List<MonthlySettlementAggregationItem> assemble(
		List<SettlementTargetSummary> summaries,
		String settlementMonth
	) {
		if (summaries == null || summaries.isEmpty()) {
			return List.of();
		}

		List<UUID> sellerIds = summaries.stream()
			.map(SettlementTargetSummary::sellerId)
			.distinct()
			.toList();

		Map<UUID, Settlement> existingSettlementBySellerId = settlementRepository.findBySettlementMonthAndSellerIds(
				settlementMonth,
				sellerIds
			).stream()
			.collect(Collectors.toMap(Settlement::getSellerId, Function.identity(), (existing, replacement) -> existing));
		Map<UUID, BigDecimal> recentThreeMonthSalesAmountBySellerId =
			findRecentThreeMonthSalesAmountBySellerIds(sellerIds, settlementMonth);
		Map<UUID, List<SettlementTargetCalculation>> calculationsBySellerId =
			settlementTargetCalculationRepository.findBySettlementMonthAndSellerIds(settlementMonth, sellerIds)
				.stream()
				.collect(Collectors.groupingBy(SettlementTargetCalculation::getSellerId));
		Map<UUID, SellerGrade> sellerGradeBySellerId = sellerGradeRepository.findBySellerIds(sellerIds)
			.stream()
			.collect(Collectors.toMap(SellerGrade::getSellerId, Function.identity(), (existing, replacement) -> existing));

		return summaries.stream()
			.map(summary -> new MonthlySettlementAggregationItem(
				summary,
				existingSettlementBySellerId.get(summary.sellerId()),
				recentThreeMonthSalesAmountBySellerId.getOrDefault(summary.sellerId(), BigDecimal.ZERO),
				calculationsBySellerId.getOrDefault(summary.sellerId(), List.of()),
				sellerGradeBySellerId.get(summary.sellerId())
			))
			.toList();
	}

	private Map<UUID, BigDecimal> findRecentThreeMonthSalesAmountBySellerIds(
		List<UUID> sellerIds,
		String settlementMonth
	) {
		YearMonth baseMonth = YearMonth.parse(settlementMonth);
		List<String> recentThreeMonths = List.of(
			baseMonth.minusMonths(2).toString(),
			baseMonth.minusMonths(1).toString(),
			baseMonth.toString()
		);

		return settlementTargetRepository.sumSettlementBaseAmountBySellerIdsAndSettlementMonths(
				sellerIds,
				recentThreeMonths
			).stream()
			.collect(Collectors.toMap(
				SellerSalesAmount::sellerId,
				SellerSalesAmount::salesAmount,
				BigDecimal::add
			));
	}
}
