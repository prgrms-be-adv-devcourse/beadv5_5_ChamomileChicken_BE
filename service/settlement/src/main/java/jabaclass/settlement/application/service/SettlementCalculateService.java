package jabaclass.settlement.application.service;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jabaclass.settlement.application.dto.SettlementTargetSummary;
import jabaclass.settlement.application.exception.BusinessException;
import jabaclass.settlement.application.exception.CommonErrorCode;
import jabaclass.settlement.application.exception.SettlementErrorCode;
import jabaclass.settlement.application.usecase.SettlementCalculateUseCase;
import jabaclass.settlement.domain.model.SellerGrade;
import jabaclass.settlement.domain.model.SellerGradePolicy;
import jabaclass.settlement.domain.model.Settlement;
import jabaclass.settlement.domain.model.SettlementHistory;
import jabaclass.settlement.domain.model.SettlementTarget;
import jabaclass.settlement.domain.model.SettlementTargetCalculation;
import jabaclass.settlement.domain.model.SettlementTargetCalculationStatus;
import jabaclass.settlement.domain.model.SettlementTargetType;
import jabaclass.settlement.domain.repository.SellerGradeRepository;
import jabaclass.settlement.domain.repository.SellerGradePolicyRepository;
import jabaclass.settlement.domain.repository.SettlementHistoryRepository;
import jabaclass.settlement.domain.repository.SettlementRepository;
import jabaclass.settlement.domain.repository.SettlementTargetCalculationRepository;
import jabaclass.settlement.domain.repository.SettlementTargetRepository;
import jabaclass.settlement.infrastructure.batch.MonthlySettlementBatchItem;
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
	private final SettlementHistoryRepository settlementHistoryRepository;
	private final SellerGradeRepository sellerGradeRepository;
	private final SellerGradePolicyRepository sellerGradePolicyRepository;

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
			BigDecimal feeAmount = calculateFeeAmount(
				summary.totalSettlementBaseAmount(),
				sellerGradePolicy.getFeeRate()
			);
			BigDecimal settlementAmount = summary.totalSettlementBaseAmount().subtract(feeAmount);
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

			List<SettlementTargetCalculation> sellerTargets =
				settlementTargetCalculationRepository.findBySettlementMonthAndSellerId(
					settlementMonth,
					summary.sellerId()
				);

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

		List<SettlementHistory> createdHistories = new ArrayList<>();
		for (int i = 0; i < savedSettlements.size(); i++) {
			createdHistories.addAll(createHistories(
				savedSettlements.get(i),
				monthlyItems.get(i).calculations()
			));
		}

		if (!createdHistories.isEmpty()) {
			settlementHistoryRepository.saveAll(createdHistories);
		}

		return savedSettlements.size();
	}

	private BigDecimal calculateRecentThreeMonthSalesAmount(UUID sellerId, String settlementMonth) {
		YearMonth baseMonth = YearMonth.parse(settlementMonth);
		List<String> recentThreeMonths = List.of(
			baseMonth.minusMonths(2).toString(),
			baseMonth.minusMonths(1).toString(),
			baseMonth.toString()
		);

		return settlementTargetRepository.sumGrossAmountBySellerIdAndSettlementMonths(sellerId, recentThreeMonths);
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
			SettlementTarget originalPaymentTarget = settlementTargetRepository.findByPaymentIdAndTargetType(
				target.getPaymentId(),
				SettlementTargetType.PAYMENT
			).orElseThrow(() -> new BusinessException(SettlementErrorCode.SETTLEMENT_NOT_FOUND));

			SettlementTargetCalculation originalPaymentCalculation =
				settlementTargetCalculationRepository.findBySettlementTargetId(
					originalPaymentTarget.getId()
				).orElseThrow(() -> new BusinessException(SettlementErrorCode.SETTLEMENT_NOT_FOUND));

			return SettlementTargetCalculation.forRefund(
				target,
				originalPaymentTarget,
				originalPaymentCalculation
			);
		}

		return SettlementTargetCalculation.forPayment(
			target,
			null,
			null
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
		BigDecimal feeAmount = calculateFeeAmount(
			summary.totalSettlementBaseAmount(),
			sellerGradePolicy.getFeeRate()
		);
		BigDecimal settlementAmount = summary.totalSettlementBaseAmount().subtract(feeAmount);
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

		List<SettlementTargetCalculation> sellerTargets =
			settlementTargetCalculationRepository.findBySettlementMonthAndSellerId(
				settlementMonth,
				summary.sellerId()
			);

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

	public List<SettlementHistory> createHistories(
		Settlement settlement,
		List<SettlementTargetCalculation> calculations
	) {
		if (calculations.isEmpty()) {
			return List.of();
		}

		List<UUID> targetIds = calculations.stream()
			.map(SettlementTargetCalculation::getSettlementTargetId)
			.toList();
		Map<UUID, SettlementTarget> targetMap = new LinkedHashMap<>();
		for (SettlementTarget target : settlementTargetRepository.findAllByIds(targetIds)) {
			targetMap.put(target.getId(), target);
		}

		List<SettlementHistory> histories = new ArrayList<>();

		for (SettlementTargetCalculation calculation : calculations) {
			SettlementTarget target = targetMap.get(calculation.getSettlementTargetId());
			if (target == null) {
				throw new BusinessException(SettlementErrorCode.SETTLEMENT_NOT_FOUND);
			}

			BigDecimal feeAmount = calculateFeeAmount(
				calculation.getSettlementBaseAmount(),
				settlement.getFeeRate()
			);
			BigDecimal settlementAmount = calculation.getSettlementBaseAmount().subtract(feeAmount);

			histories.add(SettlementHistory.create(
				settlement.getId(),
				calculation.getSettlementTargetId(),
				settlement.getSellerId(),
				target.getProductId(),
				settlement.getSettlementMonth(),
				target.getGrossAmount(),
				feeAmount,
				settlementAmount,
				settlement.getStatus()
			));
		}

		return histories;
	}

	private SellerGradePolicy resolveSellerGradePolicy(BigDecimal gradeBaseAmount) {
		return sellerGradePolicyRepository.findActiveApplicablePolicy(gradeBaseAmount)
			.or(() -> sellerGradePolicyRepository.findActiveApplicablePolicy(BigDecimal.ZERO))
			.orElseThrow(() -> new BusinessException(SettlementErrorCode.SELLER_GRADE_POLICY_NOT_FOUND));
	}

	private BigDecimal calculateFeeAmount(BigDecimal settlementBaseAmount, BigDecimal feeRate) {
		return settlementBaseAmount.multiply(feeRate).setScale(2, java.math.RoundingMode.DOWN);
	}
}
