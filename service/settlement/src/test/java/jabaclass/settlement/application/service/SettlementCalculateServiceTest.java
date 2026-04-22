package jabaclass.settlement.application.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator.ReplaceUnderscores;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import jabaclass.settlement.application.dto.AppliedPromotion;
import jabaclass.settlement.application.dto.SellerSalesAmount;
import jabaclass.settlement.application.dto.SettlementTargetSummary;
import jabaclass.settlement.application.exception.BusinessException;
import jabaclass.settlement.application.service.calculation.SettlementCalculateService;
import jabaclass.settlement.application.service.calculation.SettlementFeeCalculator;
import jabaclass.settlement.application.service.calculation.SettlementPromotionResolver;
import jabaclass.settlement.application.service.calculation.SettlementRefundCalculationService;
import jabaclass.settlement.domain.model.grade.SellerGradePolicy;
import jabaclass.settlement.domain.model.grade.SellerGradeType;
import jabaclass.settlement.domain.model.promotion.PromotionType;
import jabaclass.settlement.domain.model.settlement.Settlement;
import jabaclass.settlement.domain.model.settlement.SettlementStatus;
import jabaclass.settlement.domain.model.settlement.SettlementTarget;
import jabaclass.settlement.domain.model.settlement.SettlementTargetCalculation;
import jabaclass.settlement.domain.model.settlement.SettlementTargetType;
import jabaclass.settlement.domain.repository.SellerGradePolicyRepository;
import jabaclass.settlement.domain.repository.SellerGradeRepository;
import jabaclass.settlement.domain.repository.SettlementRepository;
import jabaclass.settlement.domain.repository.SettlementTargetCalculationRepository;
import jabaclass.settlement.domain.repository.SettlementTargetRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.never;
import static org.mockito.BDDMockito.then;

@SuppressWarnings("NonAsciiCharacters")
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayNameGeneration(ReplaceUnderscores.class)
class SettlementCalculateServiceTest {

	@Mock
	private SettlementTargetRepository settlementTargetRepository;

	@Mock
	private SettlementTargetCalculationRepository settlementTargetCalculationRepository;

	@Mock
	private SettlementRepository settlementRepository;

	@Mock
	private SellerGradeRepository sellerGradeRepository;

	@Mock
	private SellerGradePolicyRepository sellerGradePolicyRepository;

	@Mock
	private SettlementPromotionResolver settlementPromotionResolver;

	@Mock
	private SettlementRefundCalculationService settlementRefundCalculationService;

	@Mock
	private SettlementFeeCalculator settlementFeeCalculator;

	@InjectMocks
	private SettlementCalculateService settlementCalculateService;

	@Test
	void 월_정산을_계산한다() {
		UUID sellerId = UUID.randomUUID();
		String settlementMonth = "2026-03";
		UUID paymentId = UUID.randomUUID();
		SettlementTarget paymentTarget = SettlementTarget.forPayment(
			UUID.randomUUID(),
			settlementMonth,
			sellerId,
			UUID.randomUUID(),
			paymentId,
			UUID.randomUUID(),
			new BigDecimal("10000"),
			LocalDateTime.of(2026, 3, 1, 10, 0)
		);
		assignId(paymentTarget);
		SettlementTarget refundTarget = SettlementTarget.forRefund(
			UUID.randomUUID(),
			settlementMonth,
			sellerId,
			UUID.randomUUID(),
			paymentId,
			UUID.randomUUID(),
			UUID.randomUUID(),
			new BigDecimal("3000"),
			LocalDateTime.of(2026, 3, 5, 12, 0)
		);
		assignId(refundTarget);

		SettlementTargetCalculation paymentCalculation = SettlementTargetCalculation.forPayment(
			paymentTarget,
			null,
			null,
			null
		);
		SettlementTargetCalculation refundCalculation = SettlementTargetCalculation.forRefund(
			refundTarget,
			paymentTarget,
			paymentCalculation
		);

		given(settlementTargetRepository.sumSettlementBaseAmountBySellerIdsAndSettlementMonths(
			org.mockito.ArgumentMatchers.anyList(),
			org.mockito.ArgumentMatchers.anyList()
		)).willReturn(List.of(new SellerSalesAmount(sellerId, new BigDecimal("1200000"))));
		given(sellerGradePolicyRepository.findActivePolicies()).willReturn(List.of(goldPolicy(), basicPolicy()));
		given(sellerGradeRepository.findBySellerIds(org.mockito.ArgumentMatchers.anyList())).willReturn(List.of());
		given(settlementTargetCalculationRepository.findSummaryBySettlementMonth(settlementMonth))
			.willReturn(List.of(new SettlementTargetSummary(
				sellerId,
				settlementMonth,
				new BigDecimal("7000.00")
			)));
		given(settlementTargetCalculationRepository.findBySettlementMonthAndSellerIds(
			org.mockito.ArgumentMatchers.eq(settlementMonth),
			org.mockito.ArgumentMatchers.anyList()
		))
			.willReturn(List.of(paymentCalculation, refundCalculation));
		given(settlementFeeCalculator.calculateFeeAmount(
			new BigDecimal("7000.00"),
			new BigDecimal("0.025"),
			List.of(paymentCalculation, refundCalculation)
		)).willReturn(new BigDecimal("175.00"));
		given(settlementFeeCalculator.calculateSettlementAmount(
			new BigDecimal("7000.00"),
			new BigDecimal("0.025"),
			List.of(paymentCalculation, refundCalculation)
		)).willReturn(new BigDecimal("6825.00"));
		given(settlementRepository.findBySettlementMonthAndSellerIds(
			org.mockito.ArgumentMatchers.eq(settlementMonth),
			org.mockito.ArgumentMatchers.anyList()
		)).willReturn(List.of());
		given(settlementRepository.saveAll(anyList())).willAnswer(invocation -> {
			List<Settlement> settlements = invocation.getArgument(0);
			settlements.forEach(this::assignId);
			return settlements;
		});
		given(settlementTargetRepository.saveAll(anyList())).willAnswer(invocation -> invocation.getArgument(0));
		given(settlementTargetCalculationRepository.saveAll(anyList())).willAnswer(invocation -> invocation.getArgument(0));
		given(sellerGradeRepository.saveAll(org.mockito.ArgumentMatchers.anyList()))
			.willAnswer(invocation -> invocation.getArgument(0));

		ArgumentCaptor<List<Settlement>> settlementCaptor = ArgumentCaptor.forClass(List.class);

		int actual = settlementCalculateService.calculateMonthly(settlementMonth);

		assertThat(actual).isEqualTo(1);
		then(settlementRepository).should().saveAll(settlementCaptor.capture());

		Settlement settlement = settlementCaptor.getValue().get(0);
		assertThat(settlement.getOriginalAmount()).isEqualByComparingTo("7000.00");
		assertThat(settlement.getSellerGradeCode()).isEqualTo(SellerGradeType.GOLD);
		assertThat(settlement.getFeeAmount()).isEqualByComparingTo("175.00");
		assertThat(settlement.getSettlementAmount()).isEqualByComparingTo("6825.00");
	}

	@Test
	void 정산금이_0이하이면_hold_상태로_생성한다() {
		UUID sellerId = UUID.randomUUID();
		String settlementMonth = "2026-03";

		given(settlementTargetCalculationRepository.findSummaryBySettlementMonth(settlementMonth))
			.willReturn(List.of(new SettlementTargetSummary(
				sellerId,
				settlementMonth,
				new BigDecimal("-1000.00")
			)));
		given(settlementTargetCalculationRepository.findBySettlementMonthAndSellerIds(
			org.mockito.ArgumentMatchers.eq(settlementMonth),
			org.mockito.ArgumentMatchers.anyList()
		))
			.willReturn(List.of());
		given(settlementFeeCalculator.calculateFeeAmount(
			new BigDecimal("-1000.00"),
			new BigDecimal("0.033"),
			List.of()
		)).willReturn(new BigDecimal("-33.00"));
		given(settlementFeeCalculator.calculateSettlementAmount(
			new BigDecimal("-1000.00"),
			new BigDecimal("0.033"),
			List.of()
		)).willReturn(new BigDecimal("-967.00"));
		given(settlementTargetRepository.sumSettlementBaseAmountBySellerIdsAndSettlementMonths(
			org.mockito.ArgumentMatchers.anyList(),
			org.mockito.ArgumentMatchers.anyList()
		)).willReturn(List.of(new SellerSalesAmount(sellerId, new BigDecimal("100000"))));
		given(sellerGradePolicyRepository.findActivePolicies()).willReturn(List.of(basicPolicy()));
		given(settlementRepository.findBySettlementMonthAndSellerIds(
			org.mockito.ArgumentMatchers.eq(settlementMonth),
			org.mockito.ArgumentMatchers.anyList()
		)).willReturn(List.of());
		given(sellerGradeRepository.findBySellerIds(org.mockito.ArgumentMatchers.anyList())).willReturn(List.of());
		given(settlementRepository.saveAll(anyList())).willAnswer(invocation -> {
			List<Settlement> settlements = invocation.getArgument(0);
			settlements.forEach(this::assignId);
			return settlements;
		});
		given(sellerGradeRepository.saveAll(org.mockito.ArgumentMatchers.anyList()))
			.willAnswer(invocation -> invocation.getArgument(0));

		ArgumentCaptor<List<Settlement>> settlementCaptor = ArgumentCaptor.forClass(List.class);

		settlementCalculateService.calculateMonthly(settlementMonth);

		then(settlementRepository).should().saveAll(settlementCaptor.capture());
		assertThat(settlementCaptor.getValue().get(0).getStatus()).isEqualTo(SettlementStatus.HOLD);
	}

	@Test
	void 이미_정산이_존재하면_새_계산_결과로_갱신한다() {
		UUID sellerId = UUID.randomUUID();
		String settlementMonth = "2026-03";
		Settlement existingSettlement = readySettlement(sellerId, settlementMonth);

		given(settlementTargetCalculationRepository.findSummaryBySettlementMonth(settlementMonth))
			.willReturn(List.of(new SettlementTargetSummary(
				sellerId,
				settlementMonth,
				new BigDecimal("9000.00")
			)));
		given(settlementTargetCalculationRepository.findBySettlementMonthAndSellerIds(
			org.mockito.ArgumentMatchers.eq(settlementMonth),
			org.mockito.ArgumentMatchers.anyList()
		))
			.willReturn(List.of());
		given(settlementTargetRepository.sumSettlementBaseAmountBySellerIdsAndSettlementMonths(
			org.mockito.ArgumentMatchers.anyList(),
			org.mockito.ArgumentMatchers.anyList()
		)).willReturn(List.of(new SellerSalesAmount(sellerId, new BigDecimal("9000.00"))));
		given(sellerGradePolicyRepository.findActivePolicies()).willReturn(List.of(basicPolicy()));
		given(sellerGradeRepository.findBySellerIds(org.mockito.ArgumentMatchers.anyList())).willReturn(List.of());
		given(settlementFeeCalculator.calculateFeeAmount(
			new BigDecimal("9000.00"),
			new BigDecimal("0.033"),
			List.of()
		)).willReturn(new BigDecimal("297.00"));
		given(settlementFeeCalculator.calculateSettlementAmount(
			new BigDecimal("9000.00"),
			new BigDecimal("0.033"),
			List.of()
		)).willReturn(new BigDecimal("8703.00"));
		given(settlementRepository.findBySettlementMonthAndSellerIds(
			org.mockito.ArgumentMatchers.eq(settlementMonth),
			org.mockito.ArgumentMatchers.anyList()
		)).willReturn(List.of(existingSettlement));
		given(settlementRepository.saveAll(anyList())).willAnswer(invocation -> invocation.getArgument(0));
		given(sellerGradeRepository.saveAll(org.mockito.ArgumentMatchers.anyList()))
			.willAnswer(invocation -> invocation.getArgument(0));

		ArgumentCaptor<List<Settlement>> settlementCaptor = ArgumentCaptor.forClass(List.class);

		int actual = settlementCalculateService.calculateMonthly(settlementMonth);

		assertThat(actual).isEqualTo(1);
		then(settlementRepository).should().saveAll(settlementCaptor.capture());
		Settlement settlement = settlementCaptor.getValue().get(0);
		assertThat(settlement).isSameAs(existingSettlement);
		assertThat(settlement.getOriginalAmount()).isEqualByComparingTo("9000.00");
		assertThat(settlement.getFeeAmount()).isEqualByComparingTo("297.00");
		assertThat(settlement.getSettlementAmount()).isEqualByComparingTo("8703.00");
		assertThat(settlement.getStatus()).isEqualTo(SettlementStatus.READY);
	}

	@Test
	void 송금_완료된_정산은_재계산하지_않는다() {
		UUID sellerId = UUID.randomUUID();
		String settlementMonth = "2026-03";
		Settlement sentSettlement = readySettlement(sellerId, settlementMonth);
		sentSettlement.markSent(LocalDateTime.of(2026, 4, 1, 10, 0));

		given(settlementTargetCalculationRepository.findSummaryBySettlementMonth(settlementMonth))
			.willReturn(List.of(new SettlementTargetSummary(
				sellerId,
				settlementMonth,
				new BigDecimal("9000.00")
			)));
		given(settlementRepository.findBySettlementMonthAndSellerIds(
			org.mockito.ArgumentMatchers.eq(settlementMonth),
			org.mockito.ArgumentMatchers.anyList()
		)).willReturn(List.of(sentSettlement));

		int actual = settlementCalculateService.calculateMonthly(settlementMonth);

		assertThat(actual).isEqualTo(0);
		then(settlementRepository).should(never()).saveAll(anyList());
	}

	@Test
	void 정산월이_비어있으면_예외가_발생한다() {
		assertThatThrownBy(() -> settlementCalculateService.calculateMonthly(" "))
			.isInstanceOf(BusinessException.class)
			.hasMessage("파라미터 값을 확인해주세요.");
	}

	@Test
	void 적용가능한_등급정책이_없으면_기본등급으로_처리한다() {
		UUID sellerId = UUID.randomUUID();
		String settlementMonth = "2026-03";

		given(settlementTargetCalculationRepository.findSummaryBySettlementMonth(settlementMonth))
			.willReturn(List.of(new SettlementTargetSummary(
				sellerId,
				settlementMonth,
				new BigDecimal("10000.00")
			)));
		given(settlementTargetCalculationRepository.findBySettlementMonthAndSellerIds(
			org.mockito.ArgumentMatchers.eq(settlementMonth),
			org.mockito.ArgumentMatchers.anyList()
		))
			.willReturn(List.of());
		given(settlementFeeCalculator.calculateFeeAmount(
			new BigDecimal("10000.00"),
			new BigDecimal("0.033"),
			List.of()
		)).willReturn(new BigDecimal("330.00"));
		given(settlementFeeCalculator.calculateSettlementAmount(
			new BigDecimal("10000.00"),
			new BigDecimal("0.033"),
			List.of()
		)).willReturn(new BigDecimal("9670.00"));
		given(settlementTargetRepository.sumSettlementBaseAmountBySellerIdsAndSettlementMonths(
			org.mockito.ArgumentMatchers.anyList(),
			org.mockito.ArgumentMatchers.anyList()
		)).willReturn(List.of(new SellerSalesAmount(sellerId, new BigDecimal("10000"))));
		given(sellerGradePolicyRepository.findActivePolicies()).willReturn(List.of(basicPolicy()));
		given(sellerGradeRepository.findBySellerIds(org.mockito.ArgumentMatchers.anyList())).willReturn(List.of());
		given(settlementRepository.findBySettlementMonthAndSellerIds(
			org.mockito.ArgumentMatchers.eq(settlementMonth),
			org.mockito.ArgumentMatchers.anyList()
		)).willReturn(List.of());
		given(settlementRepository.saveAll(anyList())).willAnswer(invocation -> {
			List<Settlement> settlements = invocation.getArgument(0);
			settlements.forEach(this::assignId);
			return settlements;
		});
		given(sellerGradeRepository.saveAll(org.mockito.ArgumentMatchers.anyList()))
			.willAnswer(invocation -> invocation.getArgument(0));

		ArgumentCaptor<List<Settlement>> settlementCaptor = ArgumentCaptor.forClass(List.class);

		settlementCalculateService.calculateMonthly(settlementMonth);

		then(settlementRepository).should().saveAll(settlementCaptor.capture());
		Settlement settlement = settlementCaptor.getValue().get(0);
		assertThat(settlement.getSellerGradeCode()).isEqualTo(SellerGradeType.BASIC);
		assertThat(settlement.getFeeRate()).isEqualByComparingTo("0.033");
	}

	@Test
	void 결제_타겟에_프로모션이_있으면_프로모션_수수료율을_적용한다() {
		UUID sellerId = UUID.randomUUID();
		String settlementMonth = "2026-03";
		SettlementTarget paymentTarget = SettlementTarget.forPayment(
			UUID.randomUUID(),
			settlementMonth,
			sellerId,
			UUID.randomUUID(),
			UUID.randomUUID(),
			UUID.randomUUID(),
			new BigDecimal("10000.00"),
			LocalDateTime.of(2026, 3, 10, 12, 0)
		);
		assignId(paymentTarget);

		SettlementTargetCalculation calculation = SettlementTargetCalculation.forPayment(
			paymentTarget,
			UUID.randomUUID(),
			PromotionType.NEW_SELLER.name(),
			new BigDecimal("0.0100")
		);

		given(settlementPromotionResolver.resolve(sellerId, paymentTarget.getOccurredAt()))
			.willReturn(new AppliedPromotion(
				calculation.getAppliedPromotionId(),
				calculation.getAppliedPromotionType(),
				calculation.getAppliedFeeRate()
			));
		given(settlementTargetCalculationRepository.findSummaryBySettlementMonth(settlementMonth))
			.willReturn(List.of(new SettlementTargetSummary(
				sellerId,
				settlementMonth,
				new BigDecimal("10000.00")
			)));
		given(settlementTargetCalculationRepository.findBySettlementMonthAndSellerIds(
			org.mockito.ArgumentMatchers.eq(settlementMonth),
			org.mockito.ArgumentMatchers.anyList()
		))
			.willReturn(List.of(calculation));
		given(settlementFeeCalculator.calculateFeeAmount(
			new BigDecimal("10000.00"),
			new BigDecimal("0.033"),
			List.of(calculation)
		)).willReturn(new BigDecimal("100.00"));
		given(settlementFeeCalculator.calculateSettlementAmount(
			new BigDecimal("10000.00"),
			new BigDecimal("0.033"),
			List.of(calculation)
		)).willReturn(new BigDecimal("9900.00"));
		given(settlementTargetRepository.sumSettlementBaseAmountBySellerIdsAndSettlementMonths(
			org.mockito.ArgumentMatchers.anyList(),
			org.mockito.ArgumentMatchers.anyList()
		)).willReturn(List.of(new SellerSalesAmount(sellerId, new BigDecimal("10000"))));
		given(sellerGradePolicyRepository.findActivePolicies()).willReturn(List.of(basicPolicy()));
		given(sellerGradeRepository.findBySellerIds(org.mockito.ArgumentMatchers.anyList())).willReturn(List.of());
		given(settlementRepository.findBySettlementMonthAndSellerIds(
			org.mockito.ArgumentMatchers.eq(settlementMonth),
			org.mockito.ArgumentMatchers.anyList()
		)).willReturn(List.of());
		given(settlementRepository.saveAll(anyList())).willAnswer(invocation -> {
			List<Settlement> settlements = invocation.getArgument(0);
			settlements.forEach(this::assignId);
			return settlements;
		});
		given(sellerGradeRepository.saveAll(org.mockito.ArgumentMatchers.anyList()))
			.willAnswer(invocation -> invocation.getArgument(0));

		ArgumentCaptor<List<Settlement>> settlementCaptor = ArgumentCaptor.forClass(List.class);

		SettlementTargetCalculation calculated = settlementCalculateService.calculateTarget(paymentTarget);
		settlementCalculateService.calculateMonthly(settlementMonth);

		assertThat(calculated.getAppliedPromotionId()).isEqualTo(calculation.getAppliedPromotionId());
		assertThat(calculated.getAppliedFeeRate()).isEqualByComparingTo("0.0100");
		then(settlementRepository).should().saveAll(settlementCaptor.capture());
		Settlement settlement = settlementCaptor.getValue().get(0);
		assertThat(settlement.getFeeAmount()).isEqualByComparingTo("100.00");
		assertThat(settlement.getSettlementAmount()).isEqualByComparingTo("9900.00");
	}

	@Test
	void 환불_원결제_계산이_없어도_셀러_프로모션으로_2차_계산한다() {
		UUID sellerId = UUID.randomUUID();
		String settlementMonth = "2026-03";
		SettlementTarget refundTarget = SettlementTarget.forRefund(
			UUID.randomUUID(),
			settlementMonth,
			sellerId,
			UUID.randomUUID(),
			UUID.randomUUID(),
			UUID.randomUUID(),
			UUID.randomUUID(),
			new BigDecimal("3000.00"),
			LocalDateTime.of(2026, 3, 15, 12, 0)
		);
		assignId(refundTarget);

		SettlementTargetCalculation calculation = SettlementTargetCalculation.forRefundWithPromotion(
			refundTarget,
			UUID.randomUUID(),
			PromotionType.NEW_SELLER.name(),
			new BigDecimal("0.0100")
		);

		given(settlementRefundCalculationService.calculate(refundTarget)).willReturn(calculation);

		SettlementTargetCalculation actual = settlementCalculateService.calculateTarget(refundTarget);

		assertThat(actual.getSettlementBaseAmount()).isEqualByComparingTo("-3000.00");
		assertThat(actual.getAppliedPromotionId()).isEqualTo(calculation.getAppliedPromotionId());
		assertThat(actual.getAppliedFeeRate()).isEqualByComparingTo("0.0100");
	}

	private SellerGradePolicy goldPolicy() {
		SellerGradePolicy policy = new SellerGradePolicy(
			SellerGradeType.GOLD,
			1,
			new BigDecimal("1000000"),
			null,
			new BigDecimal("0.025"),
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
			new BigDecimal("0.033"),
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
			new BigDecimal("0.033"),
			new BigDecimal("6769.00")
		);
		assignId(settlement);
		return settlement;
	}

	private void assignId(Object entity) {
		ReflectionTestUtils.setField(entity, "id", UUID.randomUUID());
	}

}
