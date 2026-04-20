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
import org.mockito.junit.jupiter.MockitoExtension;

import jabaclass.settlement.application.dto.SettlementTargetSummary;
import jabaclass.settlement.application.exception.BusinessException;
import jabaclass.settlement.domain.model.Settlement;
import jabaclass.settlement.domain.model.SettlementHistory;
import jabaclass.settlement.domain.model.SettlementStatus;
import jabaclass.settlement.domain.model.SettlementTarget;
import jabaclass.settlement.domain.repository.SettlementHistoryRepository;
import jabaclass.settlement.domain.repository.SettlementRepository;
import jabaclass.settlement.domain.repository.SettlementTargetRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@SuppressWarnings("NonAsciiCharacters")
@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(ReplaceUnderscores.class)
class SettlementCalculateServiceTest {

	@Mock
	private SettlementTargetRepository settlementTargetRepository;

	@Mock
	private SettlementRepository settlementRepository;

	@Mock
	private SettlementHistoryRepository settlementHistoryRepository;

	@InjectMocks
	private SettlementCalculateService settlementCalculateService;

	@Test
	void 월_정산을_계산하고_정산_이력을_생성한다() {
		// given
		UUID sellerId = UUID.randomUUID();
		String settlementMonth = "2026-03";
		SettlementTarget paymentTarget = SettlementTarget.forPayment(
			settlementMonth,
			sellerId,
			UUID.randomUUID(),
			UUID.randomUUID(),
			UUID.randomUUID(),
			UUID.randomUUID(),
			UUID.randomUUID(),
			UUID.randomUUID(),
			2,
			new BigDecimal("5000"),
			new BigDecimal("10000"),
			LocalDateTime.of(2026, 3, 1, 10, 0)
		);
		SettlementTarget refundTarget = SettlementTarget.forRefund(
			settlementMonth,
			sellerId,
			UUID.randomUUID(),
			UUID.randomUUID(),
			UUID.randomUUID(),
			UUID.randomUUID(),
			UUID.randomUUID(),
			UUID.randomUUID(),
			UUID.randomUUID(),
			2,
			new BigDecimal("5000"),
			new BigDecimal("10000"),
			new BigDecimal("3000"),
			LocalDateTime.of(2026, 3, 5, 12, 0)
		);

		given(settlementTargetRepository.findSummaryBySettlementMonth(settlementMonth))
			.willReturn(List.of(new SettlementTargetSummary(
				sellerId,
				settlementMonth,
				new BigDecimal("7000"),
				2L,
				1L
			)));
		given(settlementTargetRepository.findBySettlementMonth(settlementMonth))
			.willReturn(List.of(paymentTarget, refundTarget));
		given(settlementRepository.existsBySellerIdAndSettlementMonth(sellerId, settlementMonth))
			.willReturn(false);
		given(settlementRepository.saveAll(anyList()))
			.willAnswer(invocation -> invocation.getArgument(0));
		given(settlementHistoryRepository.saveAll(anyList()))
			.willAnswer(invocation -> invocation.getArgument(0));

		ArgumentCaptor<List<Settlement>> settlementCaptor = ArgumentCaptor.forClass(List.class);
		ArgumentCaptor<List<SettlementHistory>> historyCaptor = ArgumentCaptor.forClass(List.class);

		// when
		int actual = settlementCalculateService.calculateMonthly(settlementMonth);

		// then
		assertThat(actual).isEqualTo(1);
		then(settlementRepository).should().saveAll(settlementCaptor.capture());
		then(settlementHistoryRepository).should().saveAll(historyCaptor.capture());

		Settlement settlement = settlementCaptor.getValue().get(0);
		assertThat(settlement.getSellerId()).isEqualTo(sellerId);
		assertThat(settlement.getOriginalAmount()).isEqualByComparingTo("7000");
		assertThat(settlement.getFeeAmount()).isEqualByComparingTo("231.00");
		assertThat(settlement.getSettlementAmount()).isEqualByComparingTo("6769.00");
		assertThat(settlement.getStatus()).isEqualTo(SettlementStatus.READY);

		List<SettlementHistory> histories = historyCaptor.getValue();
		assertThat(histories).hasSize(2);
		assertThat(histories).extracting(SettlementHistory::getOriginalAmount)
			.containsExactlyInAnyOrder(new BigDecimal("10000"), new BigDecimal("-3000"));
		assertThat(histories.stream()
			.map(SettlementHistory::getFeeAmount)
			.reduce(BigDecimal.ZERO, BigDecimal::add))
			.isEqualByComparingTo("231.00");
	}

	@Test
	void 정산금이_0이하이면_hold_상태로_생성한다() {
		// given
		UUID sellerId = UUID.randomUUID();
		String settlementMonth = "2026-03";
		SettlementTarget refundOnlyTarget = SettlementTarget.forRefund(
			settlementMonth,
			sellerId,
			UUID.randomUUID(),
			UUID.randomUUID(),
			UUID.randomUUID(),
			UUID.randomUUID(),
			UUID.randomUUID(),
			UUID.randomUUID(),
			UUID.randomUUID(),
			1,
			new BigDecimal("10000"),
			new BigDecimal("10000"),
			new BigDecimal("1000"),
			LocalDateTime.of(2026, 3, 7, 9, 0)
		);

		given(settlementTargetRepository.findSummaryBySettlementMonth(settlementMonth))
			.willReturn(List.of(new SettlementTargetSummary(
				sellerId,
				settlementMonth,
				new BigDecimal("-1000"),
				1L,
				1L
			)));
		given(settlementTargetRepository.findBySettlementMonth(settlementMonth))
			.willReturn(List.of(refundOnlyTarget));
		given(settlementRepository.existsBySellerIdAndSettlementMonth(sellerId, settlementMonth))
			.willReturn(false);
		given(settlementRepository.saveAll(anyList()))
			.willAnswer(invocation -> invocation.getArgument(0));
		given(settlementHistoryRepository.saveAll(anyList()))
			.willAnswer(invocation -> invocation.getArgument(0));

		ArgumentCaptor<List<Settlement>> settlementCaptor = ArgumentCaptor.forClass(List.class);

		// when
		settlementCalculateService.calculateMonthly(settlementMonth);

		// then
		then(settlementRepository).should().saveAll(settlementCaptor.capture());
		Settlement settlement = settlementCaptor.getValue().get(0);
		assertThat(settlement.getStatus()).isEqualTo(SettlementStatus.HOLD);
		assertThat(settlement.getFailReason()).isEqualTo("정산 금액이 0 이하이므로 송금 보류");
	}

	@Test
	void 이미_정산이_존재하면_새로_생성하지_않는다() {
		// given
		UUID sellerId = UUID.randomUUID();
		String settlementMonth = "2026-03";

		given(settlementTargetRepository.findSummaryBySettlementMonth(settlementMonth))
			.willReturn(List.of(new SettlementTargetSummary(
				sellerId,
				settlementMonth,
				new BigDecimal("7000"),
				1L,
				1L
			)));
		given(settlementTargetRepository.findBySettlementMonth(settlementMonth))
			.willReturn(List.of());
		given(settlementRepository.existsBySellerIdAndSettlementMonth(sellerId, settlementMonth))
			.willReturn(true);

		// when
		int actual = settlementCalculateService.calculateMonthly(settlementMonth);

		// then
		assertThat(actual).isEqualTo(0);
		then(settlementRepository).should(never()).saveAll(anyList());
		then(settlementHistoryRepository).should(never()).saveAll(anyList());
	}

	@Test
	void 정산월이_비어있으면_예외가_발생한다() {
		// when & then
		assertThatThrownBy(() -> settlementCalculateService.calculateMonthly(" "))
			.isInstanceOf(BusinessException.class)
			.hasMessage("파라미터 값을 확인해주세요.");
	}
}
