package jabaclass.settlement.application.service.calculation;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jabaclass.settlement.application.dto.AppliedPromotion;
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
import jabaclass.settlement.infrastructure.batch.dto.MonthlySettlementBatchItem;
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

		List<MonthlySettlementBatchItem> monthlyItems = new ArrayList<>();

		for (SettlementTargetSummary summary : summaries) {
			if (settlementRepository.existsBySellerIdAndSettlementMonth(summary.sellerId(), settlementMonth)) {
				continue;
			}

			BigDecimal recentThreeMonthSalesAmount = calculateRecentThreeMonthSalesAmount(
				summary.sellerId(),
				settlementMonth
			);
			SellerGradePolicy sellerGradePolicy = resolveSellerGradePolicy(recentThreeMonthSalesAmount);
			List<SettlementTargetCalculation> sellerTargets =
				settlementTargetCalculationRepository.findBySettlementMonthAndSellerId(
					settlementMonth,
					summary.sellerId()
				);
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
			upsertSellerGrade(summary.sellerId(), sellerGradePolicy, settlementMonth);
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

			monthlyItems.add(new MonthlySettlementBatchItem(settlement, sellerTargets));
		}

		if (monthlyItems.isEmpty()) {
			return 0;
		}

		List<Settlement> savedSettlements = settlementRepository.saveAll(
			monthlyItems.stream()
				.map(MonthlySettlementBatchItem::settlement)
				.toList()
		);

		return savedSettlements.size();
	}

	private BigDecimal calculateRecentThreeMonthSalesAmount(UUID sellerId, String settlementMonth) {
		YearMonth baseMonth = YearMonth.parse(settlementMonth);
		List<String> recentThreeMonths = List.of(
			baseMonth.minusMonths(2).toString(),
			baseMonth.minusMonths(1).toString(),
			baseMonth.toString()
		);

		return settlementTargetRepository.sumSettlementBaseAmountBySellerIdAndSettlementMonths(sellerId, recentThreeMonths);
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
	public MonthlySettlementBatchItem createMonthlySettlementItem(
		SettlementTargetSummary summary,
		String settlementMonth
	) {
		if (settlementRepository.existsBySellerIdAndSettlementMonth(summary.sellerId(), settlementMonth)) {
			return null;
		}

		BigDecimal recentThreeMonthSalesAmount = calculateRecentThreeMonthSalesAmount(
			summary.sellerId(),
			settlementMonth
		);
		SellerGradePolicy sellerGradePolicy = resolveSellerGradePolicy(recentThreeMonthSalesAmount);
		List<SettlementTargetCalculation> sellerTargets =
			settlementTargetCalculationRepository.findBySettlementMonthAndSellerId(
				settlementMonth,
				summary.sellerId()
			);
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
		upsertSellerGrade(summary.sellerId(), sellerGradePolicy, settlementMonth);

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

		return new MonthlySettlementBatchItem(
			settlement,
			sellerTargets
		);
	}

	private void upsertSellerGrade(
		UUID sellerId,
		SellerGradePolicy sellerGradePolicy,
		String settlementMonth
	) {
		SellerGrade sellerGrade = sellerGradeRepository.findBySellerId(sellerId)
			.orElseGet(() -> SellerGrade.create(
				sellerId,
				sellerGradePolicy.getId(),
				settlementMonth
			));

		sellerGrade.update(
			sellerGradePolicy.getId(),
			settlementMonth
		);

		sellerGradeRepository.save(sellerGrade);
	}

	private SellerGradePolicy resolveSellerGradePolicy(BigDecimal gradeBaseAmount) {
		return sellerGradePolicyRepository.findActiveApplicablePolicy(gradeBaseAmount)
			.or(() -> sellerGradePolicyRepository.findActiveApplicablePolicy(BigDecimal.ZERO))
			.orElseThrow(() -> new BusinessException(SettlementErrorCode.SELLER_GRADE_POLICY_NOT_FOUND));
	}
}
