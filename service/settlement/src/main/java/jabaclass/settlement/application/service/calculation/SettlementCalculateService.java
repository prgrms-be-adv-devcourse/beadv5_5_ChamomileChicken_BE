package jabaclass.settlement.application.service.calculation;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jabaclass.settlement.application.dto.AppliedPromotion;
import jabaclass.settlement.application.dto.MonthlySettlementCreationItem;
import jabaclass.settlement.application.dto.SellerGradeCalculationItem;
import jabaclass.settlement.application.dto.SettlementFeeRateAmount;
import jabaclass.settlement.application.dto.SettlementTargetInfo;
import jabaclass.settlement.application.dto.SettlementTargetSummary;
import jabaclass.settlement.application.exception.BusinessException;
import jabaclass.settlement.application.exception.SettlementCalculationRetryableException;
import jabaclass.settlement.application.exception.SettlementErrorCode;
import jabaclass.settlement.domain.model.grade.SellerGrade;
import jabaclass.settlement.domain.model.grade.SellerGradePolicy;
import jabaclass.settlement.domain.model.settlement.Settlement;
import jabaclass.settlement.domain.model.settlement.SettlementTargetCalculation;
import jabaclass.settlement.domain.repository.SellerGradePolicyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class SettlementCalculateService {

	private final SellerGradePolicyRepository sellerGradePolicyRepository;
	private final SettlementFeeCalculator settlementFeeCalculator;

	public SettlementTargetCalculation calculatePaymentTarget(
		SettlementTargetInfo target,
		AppliedPromotion appliedPromotion
	) {

		return SettlementTargetCalculation.forPayment(
			target.id(),
			target.settlementMonth(),
			target.sellerId(),
			target.settlementBaseAmount(),
			appliedPromotion.promotionId(),
			appliedPromotion.promotionType(),
			appliedPromotion.feeRate()
		);
	}

	@Transactional(
		readOnly = true,
		noRollbackFor = {
			BusinessException.class,
			SettlementCalculationRetryableException.class
		}
	)
	public SettlementTargetCalculation calculateRefundTarget(
		SettlementTargetInfo target,
		SettlementTargetInfo originalPaymentTarget,
		SettlementTargetCalculation originalPaymentCalculation,
		AppliedPromotion fallbackAppliedPromotion
	) {
		if (originalPaymentTarget != null) {
			if (originalPaymentCalculation == null) {
				throw new SettlementCalculationRetryableException(
					"원 결제 정산 계산 결과가 아직 생성되지 않았습니다."
				);
			}

			return SettlementTargetCalculation.forRefund(
				target,
				originalPaymentTarget,
				originalPaymentCalculation
			);
		}

		if (fallbackAppliedPromotion != null && fallbackAppliedPromotion.exists()) {
			return SettlementTargetCalculation.forRefundWithPromotion(
				target,
				fallbackAppliedPromotion.promotionId(),
				fallbackAppliedPromotion.promotionType(),
				fallbackAppliedPromotion.feeRate()
			);
		}

		throw new BusinessException(SettlementErrorCode.SETTLEMENT_NOT_FOUND);
	}

	public SellerGrade calculateSellerGrade(
		SellerGradeCalculationItem item,
		List<SellerGradePolicy> activeSellerGradePolicies
	) {
		SellerGradePolicy sellerGradePolicy = resolveSellerGradePolicy(
			item.recentThreeMonthSalesAmount(),
			activeSellerGradePolicies
		);

		return resolveSellerGrade(
			item.summary().sellerId(),
			sellerGradePolicy,
			item.summary().settlementMonth(),
			item.sellerGrade()
		);
	}

	public Settlement createMonthlySettlement(
		MonthlySettlementCreationItem item,
		List<SellerGradePolicy> activeSellerGradePolicies
	) {
		if (item.sellerGrade() == null) {
			return null;
		}

		Map<UUID, SellerGradePolicy> activeSellerGradePoliciesById = activeSellerGradePolicies.stream()
			.collect(Collectors.toMap(SellerGradePolicy::getId, Function.identity()));
		SellerGradePolicy sellerGradePolicy = activeSellerGradePoliciesById.get(item.sellerGrade().getSellerGradePolicyId());
		if (sellerGradePolicy == null) {
			throw new BusinessException(SettlementErrorCode.SELLER_GRADE_POLICY_NOT_FOUND);
		}

		Settlement existingSettlement = item.existingSettlement();
		if (existingSettlement != null && !existingSettlement.canRecalculate()) {
			return null;
		}

		return resolveMonthlySettlement(
			item.summary(),
			item.summary().settlementMonth(),
			item.recentThreeMonthSalesAmount(),
			sellerGradePolicy,
			item.feeRateAmounts(),
			existingSettlement
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
		List<SettlementFeeRateAmount> sellerTargets,
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
}
