package jabaclass.settlement.application.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator.ReplaceUnderscores;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import jabaclass.settlement.application.dto.AppliedPromotion;
import jabaclass.settlement.application.dto.MonthlySettlementCreationItem;
import jabaclass.settlement.application.dto.SellerGradeCalculationItem;
import jabaclass.settlement.application.dto.SettlementTargetSummary;
import jabaclass.settlement.application.exception.BusinessException;
import jabaclass.settlement.application.exception.SettlementCalculationRetryableException;
import jabaclass.settlement.application.service.calculation.SettlementCalculateService;
import jabaclass.settlement.application.service.calculation.SettlementFeeCalculator;
import jabaclass.settlement.domain.model.grade.SellerGrade;
import jabaclass.settlement.domain.model.grade.SellerGradePolicy;
import jabaclass.settlement.domain.model.grade.SellerGradeType;
import jabaclass.settlement.domain.model.promotion.PromotionType;
import jabaclass.settlement.domain.model.settlement.Settlement;
import jabaclass.settlement.domain.model.settlement.SettlementStatus;
import jabaclass.settlement.domain.model.settlement.SettlementTarget;
import jabaclass.settlement.domain.model.settlement.SettlementTargetCalculation;
import jabaclass.settlement.domain.repository.SellerGradePolicyRepository;
import jabaclass.settlement.domain.repository.SellerGradeRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@SuppressWarnings("NonAsciiCharacters")
@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(ReplaceUnderscores.class)
class SettlementCalculateServiceTest {

	@Mock
	private SellerGradeRepository sellerGradeRepository;

	@Mock
	private SellerGradePolicyRepository sellerGradePolicyRepository;

	@Mock
	private SettlementFeeCalculator settlementFeeCalculator;

	@InjectMocks
	private SettlementCalculateService settlementCalculateService;

	@Test
	void 결제_정산_계산시_프로모션_수수료율을_반영한다() {
		SettlementTarget paymentTarget = paymentTarget(new BigDecimal("10000.00"));

		SettlementTargetCalculation calculation = settlementCalculateService.calculatePaymentTarget(
			paymentTarget,
			new AppliedPromotion(
				UUID.randomUUID(),
				PromotionType.NEW_SELLER.name(),
				new BigDecimal("0.0100")
			)
		);

		assertThat(calculation.getSettlementTargetId()).isEqualTo(paymentTarget.getId());
		assertThat(calculation.getSettlementBaseAmount()).isEqualByComparingTo("10000.00");
		assertThat(calculation.getAppliedPromotionType()).isEqualTo(PromotionType.NEW_SELLER.name());
		assertThat(calculation.getAppliedFeeRate()).isEqualByComparingTo("0.0100");
	}

	@Test
	void 환불_원결제_계산결과가_없으면_retryable_예외가_발생한다() {
		SettlementTarget refundTarget = refundTarget(new BigDecimal("3000.00"));
		SettlementTarget originalPaymentTarget = paymentTarget(new BigDecimal("10000.00"));

		assertThatThrownBy(() -> settlementCalculateService.calculateRefundTarget(
			refundTarget,
			originalPaymentTarget,
			null,
			AppliedPromotion.empty()
		))
			.isInstanceOf(SettlementCalculationRetryableException.class)
			.hasMessage("원 결제 정산 계산 결과가 아직 생성되지 않았습니다.");
	}

	@Test
	void 환불은_원결제_계산결과를_기준으로_정산기준금액을_계산한다() {
		SettlementTarget paymentTarget = paymentTarget(new BigDecimal("10000.00"));
		SettlementTarget refundTarget = refundTarget(new BigDecimal("3000.00"));
		SettlementTargetCalculation paymentCalculation = SettlementTargetCalculation.forPayment(
			paymentTarget,
			UUID.randomUUID(),
			PromotionType.NEW_SELLER.name(),
			new BigDecimal("0.0100")
		);
		assignId(paymentCalculation);

		SettlementTargetCalculation refundCalculation = settlementCalculateService.calculateRefundTarget(
			refundTarget,
			paymentTarget,
			paymentCalculation,
			AppliedPromotion.empty()
		);

		assertThat(refundCalculation.getSettlementBaseAmount()).isEqualByComparingTo("-3000.00");
		assertThat(refundCalculation.getAppliedPromotionId()).isEqualTo(paymentCalculation.getAppliedPromotionId());
		assertThat(refundCalculation.getAppliedFeeRate()).isEqualByComparingTo("0.0100");
		assertThat(refundCalculation.getOriginalPaymentTargetCalculationId()).isEqualTo(paymentCalculation.getId());
	}

	@Test
	void 환불_원결제_타겟이_없으면_fallback_프로모션으로_계산한다() {
		SettlementTarget refundTarget = refundTarget(new BigDecimal("3000.00"));

		SettlementTargetCalculation refundCalculation = settlementCalculateService.calculateRefundTarget(
			refundTarget,
			null,
			null,
			new AppliedPromotion(
				UUID.randomUUID(),
				PromotionType.NEW_SELLER.name(),
				new BigDecimal("0.0100")
			)
		);

		assertThat(refundCalculation.getSettlementBaseAmount()).isEqualByComparingTo("-3000.00");
		assertThat(refundCalculation.getAppliedPromotionType()).isEqualTo(PromotionType.NEW_SELLER.name());
		assertThat(refundCalculation.getAppliedFeeRate()).isEqualByComparingTo("0.0100");
	}

	@Test
	void 최근_3개월_매출에_맞는_등급을_계산한다() {
		SellerGradePolicy goldPolicy = goldPolicy();
		SellerGradePolicy basicPolicy = basicPolicy();
		SellerGrade existingGrade = SellerGrade.create(UUID.randomUUID(), basicPolicy.getId(), "2026-03");
		SettlementTargetSummary summary = new SettlementTargetSummary(
			existingGrade.getSellerId(),
			"2026-04",
			new BigDecimal("7000.00")
		);

		SellerGrade sellerGrade = settlementCalculateService.calculateSellerGrade(
			new SellerGradeCalculationItem(summary, new BigDecimal("1200000.00"), existingGrade),
			List.of(goldPolicy, basicPolicy)
		);

		assertThat(sellerGrade).isSameAs(existingGrade);
		assertThat(sellerGrade.getSellerGradePolicyId()).isEqualTo(goldPolicy.getId());
		assertThat(sellerGrade.getCalculatedMonth()).isEqualTo("2026-04");
	}

	@Test
	void seller_grade가_없으면_월정산을_생성하지_않는다() {
		MonthlySettlementCreationItem item = new MonthlySettlementCreationItem(
			new SettlementTargetSummary(UUID.randomUUID(), "2026-04", new BigDecimal("7000.00")),
			null,
			new BigDecimal("100000.00"),
			List.of(),
			null
		);

		Settlement settlement = settlementCalculateService.createMonthlySettlement(item, List.of(basicPolicy()));

		assertThat(settlement).isNull();
	}

	@Test
	void 정산금이_0이하면_hold_상태로_생성한다() {
		SellerGradePolicy basicPolicy = basicPolicy();
		SellerGrade sellerGrade = SellerGrade.create(UUID.randomUUID(), basicPolicy.getId(), "2026-04");
		SettlementTargetSummary summary = new SettlementTargetSummary(
			sellerGrade.getSellerId(),
			"2026-04",
			new BigDecimal("1000.00")
		);
		MonthlySettlementCreationItem item = new MonthlySettlementCreationItem(
			summary,
			null,
			new BigDecimal("5000.00"),
			List.of(),
			sellerGrade
		);
		given(settlementFeeCalculator.calculateFeeAmount(
			new BigDecimal("1000.00"),
			new BigDecimal("0.0330"),
			List.of()
		)).willReturn(new BigDecimal("33.00"));
		given(settlementFeeCalculator.calculateSettlementAmount(
			new BigDecimal("1000.00"),
			new BigDecimal("0.0330"),
			List.of()
		)).willReturn(BigDecimal.ZERO);

		Settlement settlement = settlementCalculateService.createMonthlySettlement(item, List.of(basicPolicy));

		assertThat(settlement).isNotNull();
		assertThat(settlement.getStatus()).isEqualTo(SettlementStatus.HOLD);
		assertThat(settlement.getFailReason()).isEqualTo("정산 금액이 0 이하이므로 송금 보류");
	}

	@Test
	void 송금_완료된_정산은_재계산하지_않는다() {
		SellerGradePolicy basicPolicy = basicPolicy();
		SellerGrade sellerGrade = SellerGrade.create(UUID.randomUUID(), basicPolicy.getId(), "2026-04");
		Settlement existingSettlement = readySettlement(sellerGrade.getSellerId(), "2026-04");
		existingSettlement.markSent(LocalDateTime.of(2026, 5, 1, 10, 0));
		MonthlySettlementCreationItem item = new MonthlySettlementCreationItem(
			new SettlementTargetSummary(sellerGrade.getSellerId(), "2026-04", new BigDecimal("9000.00")),
			existingSettlement,
			new BigDecimal("9000.00"),
			List.of(),
			sellerGrade
		);

		Settlement settlement = settlementCalculateService.createMonthlySettlement(item, List.of(basicPolicy));

		assertThat(settlement).isNull();
	}

	@Test
	void 활성_등급정책_조회는_repository를_위임한다() {
		List<SellerGradePolicy> policies = List.of(goldPolicy(), basicPolicy());
		given(sellerGradePolicyRepository.findActivePolicies()).willReturn(policies);

		List<SellerGradePolicy> actual = settlementCalculateService.findActiveSellerGradePolicies();

		assertThat(actual).containsExactlyElementsOf(policies);
		then(sellerGradePolicyRepository).should().findActivePolicies();
	}

	private SettlementTarget paymentTarget(BigDecimal amount) {
		SettlementTarget target = SettlementTarget.forPayment(
			UUID.randomUUID(),
			"2026-04",
			UUID.randomUUID(),
			UUID.randomUUID(),
			UUID.randomUUID(),
			UUID.randomUUID(),
			amount,
			LocalDateTime.of(2026, 4, 10, 12, 0)
		);
		assignId(target);
		return target;
	}

	private SettlementTarget refundTarget(BigDecimal amount) {
		SettlementTarget target = SettlementTarget.forRefund(
			UUID.randomUUID(),
			"2026-04",
			UUID.randomUUID(),
			UUID.randomUUID(),
			UUID.randomUUID(),
			UUID.randomUUID(),
			UUID.randomUUID(),
			amount,
			LocalDateTime.of(2026, 4, 15, 12, 0)
		);
		assignId(target);
		return target;
	}

	private SellerGradePolicy goldPolicy() {
		SellerGradePolicy policy = new SellerGradePolicy(
			SellerGradeType.GOLD,
			1,
			new BigDecimal("1000000.00"),
			null,
			new BigDecimal("0.0250"),
			true,
			LocalDateTime.of(2026, 1, 1, 0, 0),
			null
		);
		assignId(policy);
		return policy;
	}

	private SellerGradePolicy basicPolicy() {
		SellerGradePolicy policy = new SellerGradePolicy(
			SellerGradeType.BASIC,
			1,
			BigDecimal.ZERO,
			new BigDecimal("999999.99"),
			new BigDecimal("0.0330"),
			true,
			LocalDateTime.of(2026, 1, 1, 0, 0),
			null
		);
		assignId(policy);
		return policy;
	}

	private Settlement readySettlement(UUID sellerId, String settlementMonth) {
		Settlement settlement = Settlement.createReady(
			sellerId,
			settlementMonth,
			new BigDecimal("7000.00"),
			SellerGradeType.BASIC,
			UUID.randomUUID(),
			new BigDecimal("7000.00"),
			new BigDecimal("231.00"),
			new BigDecimal("0.0330"),
			new BigDecimal("6769.00")
		);
		assignId(settlement);
		return settlement;
	}

	private void assignId(Object entity) {
		ReflectionTestUtils.setField(entity, "id", UUID.randomUUID());
	}
}
