package jabaclass.settlement.application.service.calculation;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jabaclass.settlement.application.dto.AppliedPromotion;
import jabaclass.settlement.application.dto.MonthlySettlementAggregationItem;
import jabaclass.settlement.application.dto.MonthlySettlementAggregationResult;
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
	private final SettlementAggregationItemAssembler settlementAggregationItemAssembler;

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
		return createAndSaveMonthlySettlements(
			summaries,
			settlementMonth,
			findActiveSellerGradePolicies()
		);
	}

	@Transactional
	public List<Settlement> createAndSaveMonthlySettlements(
		List<SettlementTargetSummary> summaries,
		String settlementMonth,
		List<SellerGradePolicy> activeSellerGradePolicies
	) {
		MonthlySettlementCreationResult creationResult = createMonthlySettlementCreationResult(
			summaries,
			settlementMonth,
			activeSellerGradePolicies
		);
		if (creationResult.isEmpty()) {
			return List.of();
		}

		if (!creationResult.sellerGrades().isEmpty()) {
			sellerGradeRepository.saveAll(creationResult.sellerGrades());
		}

		return settlementRepository.saveAll(creationResult.settlements());
	}

	@Transactional
	public List<Settlement> createMonthlySettlements(
		List<SettlementTargetSummary> summaries,
		String settlementMonth
	) {
		return createMonthlySettlements(
			summaries,
			settlementMonth,
			findActiveSellerGradePolicies()
		);
	}

	@Transactional
	public List<Settlement> createMonthlySettlements(
		List<SettlementTargetSummary> summaries,
		String settlementMonth,
		List<SellerGradePolicy> activeSellerGradePolicies
	) {
		return createMonthlySettlementCreationResult(
			summaries,
			settlementMonth,
			activeSellerGradePolicies
		).settlements();
	}

	private MonthlySettlementCreationResult createMonthlySettlementCreationResult(
		List<SettlementTargetSummary> summaries,
		String settlementMonth,
		List<SellerGradePolicy> activeSellerGradePolicies
	) {
		if (summaries == null || summaries.isEmpty()) {
			return MonthlySettlementCreationResult.empty();
		}

		List<MonthlySettlementAggregationItem> aggregationItems =
			settlementAggregationItemAssembler.assemble(summaries, settlementMonth);

		List<Settlement> settlements = new ArrayList<>();
		List<SellerGrade> sellerGrades = new ArrayList<>();
		for (MonthlySettlementAggregationItem aggregationItem : aggregationItems) {
			MonthlySettlementAggregationResult result = aggregateMonthlySettlement(
				aggregationItem,
				activeSellerGradePolicies
			);
			if (result == null) {
				continue;
			}
			settlements.add(result.settlement());
			sellerGrades.add(result.sellerGrade());
		}

		return new MonthlySettlementCreationResult(settlements, sellerGrades);
	}

	public MonthlySettlementAggregationResult aggregateMonthlySettlement(
		MonthlySettlementAggregationItem item,
		List<SellerGradePolicy> activeSellerGradePolicies
	) {
		SettlementTargetSummary summary = item.summary();
		Settlement existingSettlement = item.existingSettlement();

		if (existingSettlement != null && !existingSettlement.canRecalculate()) {
			return null;
		}

		SellerGradePolicy sellerGradePolicy = resolveSellerGradePolicy(
			item.recentThreeMonthSalesAmount(),
			activeSellerGradePolicies
		);

		return new MonthlySettlementAggregationResult(
			resolveMonthlySettlement(
				summary,
				summary.settlementMonth(),
				item.recentThreeMonthSalesAmount(),
				sellerGradePolicy,
				item.calculations(),
				existingSettlement
			),
			resolveSellerGrade(
				summary.sellerId(),
				sellerGradePolicy,
				summary.settlementMonth(),
				item.sellerGrade()
			)
		);
	}

	public List<SellerGradePolicy> findActiveSellerGradePolicies() {
		return sellerGradePolicyRepository.findActivePolicies();
	}

	private Settlement resolveMonthlySettlement(
		SettlementTargetSummary summary,
		String settlementMonth,
		BigDecimal recentThreeMonthSalesAmount,
		SellerGradePolicy sellerGradePolicy,
		List<SettlementTargetCalculation> sellerTargets,
		Settlement existingSettlement
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

		Settlement settlement;
		if (existingSettlement == null) {
			settlement = Settlement.createReady(
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
		} else {
			existingSettlement.recalculate(
				summary.totalSettlementBaseAmount(),
				sellerGradePolicy.getGradeCode(),
				sellerGradePolicy.getId(),
				recentThreeMonthSalesAmount,
				feeAmount,
				sellerGradePolicy.getFeeRate(),
				settlementAmount
			);
			settlement = existingSettlement;
		}

		if (!settlement.isTransferable()) {
			settlement.hold("정산 금액이 0 이하이므로 송금 보류");
		}

		return settlement;
	}

	private SellerGrade resolveSellerGrade(
		UUID sellerId,
		SellerGradePolicy sellerGradePolicy,
		String settlementMonth,
		SellerGrade existingSellerGrade
	) {
		SellerGrade sellerGrade = existingSellerGrade != null
			? existingSellerGrade
			: SellerGrade.create(
				sellerId,
				sellerGradePolicy.getId(),
				settlementMonth
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

	private record MonthlySettlementCreationResult(
		List<Settlement> settlements,
		List<SellerGrade> sellerGrades
	) {

		private static MonthlySettlementCreationResult empty() {
			return new MonthlySettlementCreationResult(List.of(), List.of());
		}

		private boolean isEmpty() {
			return settlements.isEmpty() && sellerGrades.isEmpty();
		}
	}
}
