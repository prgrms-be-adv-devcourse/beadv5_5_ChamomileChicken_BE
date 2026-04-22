package jabaclass.settlement.application.service.calculation;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jabaclass.settlement.application.dto.AppliedPromotion;
import jabaclass.settlement.application.dto.SellerSalesAmount;
import jabaclass.settlement.application.dto.SettlementTargetSummary;
import jabaclass.settlement.application.exception.BusinessException;
import jabaclass.settlement.application.exception.CommonErrorCode;
import jabaclass.settlement.application.exception.SettlementErrorCode;
import jabaclass.settlement.application.usecase.SettlementCalculateUseCase;
import jabaclass.settlement.domain.model.grade.SellerGrade;
import jabaclass.settlement.domain.model.grade.SellerGradePolicy;
import jabaclass.settlement.domain.model.settlement.Settlement;
import jabaclass.settlement.domain.model.settlement.SettlementTarget;
import jabaclass.settlement.domain.model.settlement.SettlementTargetCalculation;
import jabaclass.settlement.domain.model.settlement.SettlementTargetCalculationStatus;
import jabaclass.settlement.domain.model.settlement.SettlementTargetType;
import jabaclass.settlement.domain.repository.SellerGradeRepository;
import jabaclass.settlement.domain.repository.SellerGradePolicyRepository;
import jabaclass.settlement.domain.repository.SettlementRepository;
import jabaclass.settlement.domain.repository.SettlementTargetCalculationRepository;
import jabaclass.settlement.domain.repository.SettlementTargetRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class SettlementCalculateService implements SettlementCalculateUseCase {

	private final SettlementTargetRepository settlementTargetRepository;
	private final SettlementTargetCalculationRepository settlementTargetCalculationRepository;
	private final SettlementRepository settlementRepository;
	private final SellerGradeRepository sellerGradeRepository;
	private final SellerGradePolicyRepository sellerGradePolicyRepository;
	private final SettlementPromotionResolver settlementPromotionResolver;
	private final SettlementRefundCalculationService settlementRefundCalculationService;
	private final SettlementFeeCalculator settlementFeeCalculator;

	@Override
	@Transactional
	public int calculateMonthly(String settlementMonth) {
		if (settlementMonth == null || settlementMonth.isBlank()) {
			throw new BusinessException(CommonErrorCode.INVALID_PARAMETER);
		}

		List<SettlementTargetSummary> summaries =
			settlementTargetCalculationRepository.findSummaryBySettlementMonth(settlementMonth);

		return createAndSaveMonthlySettlements(summaries, settlementMonth).size();
	}

	public List<SettlementTarget> findPendingTargets(String settlementMonth) {
		return settlementTargetRepository.findBySettlementMonthAndCalculationStatus(
			settlementMonth,
			SettlementTargetCalculationStatus.PENDING
		);
	}

	@Transactional
	public SettlementTargetCalculation calculateTarget(SettlementTarget target) {
		if (settlementTargetCalculationRepository.existsBySettlementTargetId(target.getId())) {
			return null;
		}

		if (target.getTargetType() == SettlementTargetType.REFUND) {
			return settlementRefundCalculationService.calculate(target);
		}

		AppliedPromotion appliedPromotion = settlementPromotionResolver.resolve(target.getSellerId(), target.getOccurredAt());

		return SettlementTargetCalculation.forPayment(
			target,
			appliedPromotion.promotionId(),
			appliedPromotion.promotionType(),
			appliedPromotion.feeRate()
		);
	}

	public void markTargetCalculated(SettlementTarget target) {
		target.markCalculated();
	}

	public void markTargetCalculationFailed(SettlementTarget target, Exception e) {
		target.markCalculationFailed(e.getMessage());
		log.error("[SETTLEMENT_CALCULATION] targetId={} 계산 실패", target.getId(), e);
	}

	@Transactional
	public Settlement createMonthlySettlement(
		SettlementTargetSummary summary,
		String settlementMonth
	) {
		List<Settlement> settlements = createMonthlySettlements(List.of(summary), settlementMonth);
		if (settlements.isEmpty()) {
			return null;
		}

		return settlements.get(0);
	}

	@Transactional
	public List<Settlement> createAndSaveMonthlySettlements(
		List<SettlementTargetSummary> summaries,
		String settlementMonth
	) {
		List<Settlement> settlements = createMonthlySettlements(summaries, settlementMonth);
		if (settlements.isEmpty()) {
			return List.of();
		}

		return settlementRepository.saveAll(settlements);
	}

	@Transactional
	public List<Settlement> createMonthlySettlements(
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
		Set<UUID> existingSettlementSellerIds = settlementRepository.findBySettlementMonthAndSellerIds(
				settlementMonth,
				sellerIds
			).stream()
			.map(Settlement::getSellerId)
			.collect(Collectors.toSet());
		Map<UUID, BigDecimal> recentThreeMonthSalesAmountBySellerId =
			findRecentThreeMonthSalesAmountBySellerIds(sellerIds, settlementMonth);
		Map<UUID, List<SettlementTargetCalculation>> calculationsBySellerId =
			settlementTargetCalculationRepository.findBySettlementMonthAndSellerIds(settlementMonth, sellerIds)
				.stream()
				.collect(Collectors.groupingBy(SettlementTargetCalculation::getSellerId));
		Map<UUID, SellerGrade> sellerGradeBySellerId = sellerGradeRepository.findBySellerIds(sellerIds)
			.stream()
			.collect(Collectors.toMap(SellerGrade::getSellerId, Function.identity(), (existing, replacement) -> existing));
		List<SellerGradePolicy> activeSellerGradePolicies = sellerGradePolicyRepository.findActivePolicies();

		List<Settlement> settlements = new ArrayList<>();
		List<SellerGrade> sellerGrades = new ArrayList<>();
		for (SettlementTargetSummary summary : summaries) {
			if (existingSettlementSellerIds.contains(summary.sellerId())) {
				continue;
			}

			BigDecimal recentThreeMonthSalesAmount = recentThreeMonthSalesAmountBySellerId.getOrDefault(
				summary.sellerId(),
				BigDecimal.ZERO
			);
			SellerGradePolicy sellerGradePolicy = resolveSellerGradePolicy(
				recentThreeMonthSalesAmount,
				activeSellerGradePolicies
			);
			List<SettlementTargetCalculation> sellerTargets = calculationsBySellerId.getOrDefault(
				summary.sellerId(),
				List.of()
			);

			settlements.add(buildMonthlySettlement(
				summary,
				settlementMonth,
				recentThreeMonthSalesAmount,
				sellerGradePolicy,
				sellerTargets
			));
			sellerGrades.add(resolveSellerGrade(
				summary.sellerId(),
				sellerGradePolicy,
				settlementMonth,
				sellerGradeBySellerId
			));
		}

		if (!sellerGrades.isEmpty()) {
			sellerGradeRepository.saveAll(sellerGrades);
		}

		return settlements;
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

	private Settlement buildMonthlySettlement(
		SettlementTargetSummary summary,
		String settlementMonth,
		BigDecimal recentThreeMonthSalesAmount,
		SellerGradePolicy sellerGradePolicy,
		List<SettlementTargetCalculation> sellerTargets
	) {

		BigDecimal feeAmount = settlementFeeCalculator.calculateFeeAmount(
			summary.totalSettlementBaseAmount(),
			sellerGradePolicy.getFeeRate(),
			sellerTargets
		);
		BigDecimal settlementAmount = settlementFeeCalculator.calculateSettlementAmount(
			summary.totalSettlementBaseAmount(),
			sellerGradePolicy.getFeeRate(),
			sellerTargets
		);

		Settlement settlement = Settlement.createReady(
			summary.sellerId(),
			settlementMonth,
			summary.totalSettlementBaseAmount(),
			sellerGradePolicy.getGradeCode(),
			sellerGradePolicy.getId(),
			recentThreeMonthSalesAmount,
			feeAmount,
			sellerGradePolicy.getFeeRate(),
			settlementAmount
		);

		if (!settlement.isTransferable()) {
			settlement.hold("정산 금액이 0 이하이므로 송금 보류");
		}

		return settlement;
	}

	private SellerGrade resolveSellerGrade(
		UUID sellerId,
		SellerGradePolicy sellerGradePolicy,
		String settlementMonth,
		Map<UUID, SellerGrade> sellerGradeBySellerId
	) {
		SellerGrade sellerGrade = sellerGradeBySellerId.getOrDefault(
			sellerId,
			SellerGrade.create(
				sellerId,
				sellerGradePolicy.getId(),
				settlementMonth
			)
		);

		sellerGrade.update(
			sellerGradePolicy.getId(),
			settlementMonth
		);

		return sellerGrade;
	}

	private SellerGradePolicy resolveSellerGradePolicy(
		BigDecimal gradeBaseAmount,
		List<SellerGradePolicy> activeSellerGradePolicies
	) {
		return activeSellerGradePolicies.stream()
			.filter(policy -> policy.getMinSalesAmount().compareTo(gradeBaseAmount) <= 0)
			.filter(policy -> policy.getMaxSalesAmount() == null
				|| policy.getMaxSalesAmount().compareTo(gradeBaseAmount) >= 0)
			.findFirst()
			.or(() -> activeSellerGradePolicies.stream()
				.filter(policy -> policy.getMinSalesAmount().compareTo(BigDecimal.ZERO) <= 0)
				.filter(policy -> policy.getMaxSalesAmount() == null
					|| policy.getMaxSalesAmount().compareTo(BigDecimal.ZERO) >= 0)
				.findFirst())
			.orElseThrow(() -> new BusinessException(SettlementErrorCode.SELLER_GRADE_POLICY_NOT_FOUND));
	}
}
