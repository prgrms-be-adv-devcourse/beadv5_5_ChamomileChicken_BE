package jabaclass.settlement.application.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator.ReplaceUnderscores;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import jabaclass.settlement.application.dto.SellerSettlementAccount;
import jabaclass.settlement.application.dto.SettlementTransferResult;
import jabaclass.settlement.application.exception.BusinessException;
import jabaclass.settlement.application.port.external.SellerSettlementPort;
import jabaclass.settlement.application.port.external.SettlementTransferPort;
import jabaclass.settlement.domain.model.grade.SellerGradeType;
import jabaclass.settlement.domain.model.settlement.Settlement;
import jabaclass.settlement.domain.model.settlement.SettlementStatus;
import jabaclass.settlement.domain.model.settlement.SettlementTransfer;
import jabaclass.settlement.domain.repository.SettlementRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.atLeastOnce;

@SuppressWarnings("NonAsciiCharacters")
@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(ReplaceUnderscores.class)
class SettlementTransferServiceTest {

	private static final UUID SELLER_GRADE_POLICY_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

	@Mock
	private SettlementRepository settlementRepository;

	@Mock
	private SettlementTransferStatePersistenceService settlementTransferStatePersistenceService;

	@Mock
	private SellerSettlementPort sellerSettlementPort;

	@Mock
	private SettlementTransferPort settlementTransferPort;

	@InjectMocks
	private SettlementTransferService settlementTransferService;

	@Test
	void ready_정산을_송금_성공처리한다() {
		// given
		String settlementMonth = "2026-03";
		UUID sellerId = UUID.randomUUID();
		Settlement settlement = Settlement.createReady(
			sellerId,
			settlementMonth,
			new BigDecimal("10000"),
			SellerGradeType.BASIC,
			SELLER_GRADE_POLICY_ID,
			new BigDecimal("300000"),
			new BigDecimal("330.00"),
			new BigDecimal("0.033"),
			new BigDecimal("9670.00")
		);
		assignId(settlement);

		given(settlementRepository.findBySettlementMonthAndStatus(settlementMonth, SettlementStatus.READY))
			.willReturn(List.of(settlement));
		given(sellerSettlementPort.fetchSellerSettlementAccounts(Set.of(sellerId)))
			.willReturn(List.of(new SellerSettlementAccount(
				sellerId,
				"088",
				"123-456",
				"판매자",
				true
			)));
		given(settlementTransferPort.transfer(any()))
			.willReturn(SettlementTransferResult.ok());

		ArgumentCaptor<Settlement> settlementCaptor = ArgumentCaptor.forClass(Settlement.class);

		// when
		int actual = settlementTransferService.transferMonthly(settlementMonth);

		// then
		assertThat(actual).isEqualTo(1);
		then(settlementTransferStatePersistenceService).should(atLeastOnce())
			.saveTransferState(settlementCaptor.capture(), any());
		Settlement savedSettlement = settlementCaptor.getAllValues().getLast();
		assertThat(savedSettlement.getStatus()).isEqualTo(SettlementStatus.SENT);
		assertThat(savedSettlement.getTransferredAt()).isNotNull();
	}

	@Test
	void 판매자_계좌가_없으면_hold_처리한다() {
		// given
		String settlementMonth = "2026-03";
		UUID sellerId = UUID.randomUUID();
		Settlement settlement = Settlement.createReady(
			sellerId,
			settlementMonth,
			new BigDecimal("10000"),
			SellerGradeType.BASIC,
			SELLER_GRADE_POLICY_ID,
			new BigDecimal("300000"),
			new BigDecimal("330.00"),
			new BigDecimal("0.033"),
			new BigDecimal("9670.00")
		);
		assignId(settlement);

		given(settlementRepository.findBySettlementMonthAndStatus(settlementMonth, SettlementStatus.READY))
			.willReturn(List.of(settlement));
		given(sellerSettlementPort.fetchSellerSettlementAccounts(Set.of(sellerId)))
			.willReturn(List.of());

		ArgumentCaptor<Settlement> settlementCaptor = ArgumentCaptor.forClass(Settlement.class);

		// when
		int actual = settlementTransferService.transferMonthly(settlementMonth);

		// then
		assertThat(actual).isEqualTo(0);
		then(settlementTransferStatePersistenceService).should().saveTransferState(settlementCaptor.capture(), any());
		Settlement savedSettlement = settlementCaptor.getValue();
		assertThat(savedSettlement.getStatus()).isEqualTo(SettlementStatus.HOLD);
		assertThat(savedSettlement.getFailReason()).isEqualTo("판매자 정산 계좌 정보가 없습니다.");
	}

	@Test
	void 판매자_계좌가_비활성이면_hold_처리한다() {
		// given
		String settlementMonth = "2026-03";
		UUID sellerId = UUID.randomUUID();
		Settlement settlement = Settlement.createReady(
			sellerId,
			settlementMonth,
			new BigDecimal("10000"),
			SellerGradeType.BASIC,
			SELLER_GRADE_POLICY_ID,
			new BigDecimal("300000"),
			new BigDecimal("330.00"),
			new BigDecimal("0.033"),
			new BigDecimal("9670.00")
		);
		assignId(settlement);

		given(settlementRepository.findBySettlementMonthAndStatus(settlementMonth, SettlementStatus.READY))
			.willReturn(List.of(settlement));
		given(sellerSettlementPort.fetchSellerSettlementAccounts(Set.of(sellerId)))
			.willReturn(List.of(new SellerSettlementAccount(
				sellerId,
				"",
				"123-456",
				"판매자",
				true
			)));

		ArgumentCaptor<Settlement> settlementCaptor = ArgumentCaptor.forClass(Settlement.class);

		// when
		settlementTransferService.transferMonthly(settlementMonth);

		// then
		then(settlementTransferStatePersistenceService).should().saveTransferState(settlementCaptor.capture(), any());
		Settlement savedSettlement = settlementCaptor.getValue();
		assertThat(savedSettlement.getStatus()).isEqualTo(SettlementStatus.HOLD);
		assertThat(savedSettlement.getFailReason()).isEqualTo("판매자 정산 계좌가 비활성 상태입니다.");
	}

	@Test
	void 송금_실패_응답이면_failed_처리한다() {
		// given
		String settlementMonth = "2026-03";
		UUID sellerId = UUID.randomUUID();
		Settlement settlement = Settlement.createReady(
			sellerId,
			settlementMonth,
			new BigDecimal("10000"),
			SellerGradeType.BASIC,
			SELLER_GRADE_POLICY_ID,
			new BigDecimal("300000"),
			new BigDecimal("330.00"),
			new BigDecimal("0.033"),
			new BigDecimal("9670.00")
		);
		assignId(settlement);

		given(settlementRepository.findBySettlementMonthAndStatus(settlementMonth, SettlementStatus.READY))
			.willReturn(List.of(settlement));
		given(sellerSettlementPort.fetchSellerSettlementAccounts(Set.of(sellerId)))
			.willReturn(List.of(new SellerSettlementAccount(
				sellerId,
				"088",
				"123-456",
				"판매자",
				true
			)));
		given(settlementTransferPort.transfer(any()))
			.willReturn(SettlementTransferResult.fail("송금 실패"));

		ArgumentCaptor<Settlement> settlementCaptor = ArgumentCaptor.forClass(Settlement.class);

		// when
		int actual = settlementTransferService.transferMonthly(settlementMonth);

		// then
		assertThat(actual).isEqualTo(0);
		then(settlementTransferStatePersistenceService).should(atLeastOnce())
			.saveTransferState(settlementCaptor.capture(), any());
		Settlement savedSettlement = settlementCaptor.getAllValues().getLast();
		assertThat(savedSettlement.getStatus()).isEqualTo(SettlementStatus.FAILED);
		assertThat(savedSettlement.getFailReason()).isEqualTo("송금 실패");
	}

	@Test
	void 계좌번호는_뒤_4자리를_제외하고_마스킹한다() {
		SettlementTransfer transfer = SettlementTransfer.requested(
			UUID.randomUUID(),
			"088",
			"1234567890",
			new BigDecimal("10000")
		);

		assertThat(transfer.getAccountNumberMasked()).isEqualTo("******7890");
	}

	@Test
	void 계좌번호가_4자리_이하이면_전체를_마스킹한다() {
		SettlementTransfer transfer = SettlementTransfer.requested(
			UUID.randomUUID(),
			"088",
			"1234",
			new BigDecimal("10000")
		);

		assertThat(transfer.getAccountNumberMasked()).isEqualTo("****");
	}

	@Test
	void 정산월이_비어있으면_예외가_발생한다() {
		// when & then
		assertThatThrownBy(() -> settlementTransferService.transferMonthly(""))
			.isInstanceOf(BusinessException.class)
			.hasMessage("파라미터 값을 확인해주세요.");
	}

	private void assignId(Settlement settlement) {
		ReflectionTestUtils.setField(settlement, "id", UUID.randomUUID());
	}
}
