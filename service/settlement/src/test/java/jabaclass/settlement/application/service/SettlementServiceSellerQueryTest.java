package jabaclass.settlement.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator.ReplaceUnderscores;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

import jabaclass.settlement.application.exception.BusinessException;
import jabaclass.settlement.application.exception.SettlementErrorCode;
import jabaclass.settlement.domain.model.grade.SellerGradeType;
import jabaclass.settlement.domain.model.settlement.Settlement;
import jabaclass.settlement.domain.model.settlement.SettlementStatus;
import jabaclass.settlement.domain.model.settlement.SettlementTarget;
import jabaclass.settlement.domain.model.settlement.SettlementTargetCalculation;
import jabaclass.settlement.domain.repository.SettlementRepository;
import jabaclass.settlement.domain.repository.SettlementTargetCalculationRepository;
import jabaclass.settlement.domain.repository.SettlementTargetRepository;
import jabaclass.settlement.presentation.dto.response.SellerSettlementDetailPageResponse;
import jabaclass.settlement.presentation.dto.response.SellerSettlementPageResponse;

@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(ReplaceUnderscores.class)
@SuppressWarnings("NonAsciiCharacters")
class SettlementServiceSellerQueryTest {

	@Mock
	private SettlementRepository settlementRepository;

	@Mock
	private SettlementTargetCalculationRepository settlementTargetCalculationRepository;

	@Mock
	private SettlementTargetRepository settlementTargetRepository;

	@InjectMocks
	private SettlementService settlementService;

	@Test
	void 판매자_정산_목록을_페이지로_조회한다() {
		UUID sellerId = UUID.randomUUID();
		Settlement settlement = settlement(sellerId, "2026-03");

		given(settlementRepository.findBySellerId(org.mockito.ArgumentMatchers.eq(sellerId), org.mockito.ArgumentMatchers.any()))
			.willReturn(new PageImpl<>(List.of(settlement), PageRequest.of(0, 20), 1));

		SellerSettlementPageResponse response = settlementService.getSellerSettlements(sellerId, 0, 20);

		assertThat(response.items()).hasSize(1);
		assertThat(response.items().get(0).sellerId()).isEqualTo(sellerId);
		assertThat(response.page()).isEqualTo(0);
		assertThat(response.totalElements()).isEqualTo(1);
	}

	@Test
	void 판매자_정산_상세_항목을_페이지로_조회한다() {
		UUID sellerId = UUID.randomUUID();
		Settlement settlement = settlement(sellerId, "2026-03");
		UUID settlementId = UUID.randomUUID();
		ReflectionTestUtils.setField(settlement, "id", settlementId);

		SettlementTarget target = SettlementTarget.forPayment(
			UUID.randomUUID(),
			"2026-03",
			sellerId,
			UUID.randomUUID(),
			UUID.randomUUID(),
			UUID.randomUUID(),
			new BigDecimal("15000"),
			LocalDateTime.of(2026, 3, 10, 10, 0)
		);
		assignId(target);

		SettlementTargetCalculation calculation = SettlementTargetCalculation.forPayment(
			target.getId(),
			target.getSettlementMonth(),
			target.getSellerId(),
			target.getSettlementBaseAmount(),
			UUID.randomUUID(),
			"NEW_SELLER",
			new BigDecimal("0.0100")
		);
		assignId(calculation);

		given(settlementRepository.findById(settlementId)).willReturn(Optional.of(settlement));
		given(settlementTargetCalculationRepository.findBySettlementMonthAndSellerId(
			org.mockito.ArgumentMatchers.eq("2026-03"),
			org.mockito.ArgumentMatchers.eq(sellerId),
			org.mockito.ArgumentMatchers.any()
		)).willReturn(new PageImpl<>(List.of(calculation), PageRequest.of(0, 20), 1));
		given(settlementTargetRepository.findAllByIds(List.of(target.getId()))).willReturn(List.of(target));

		SellerSettlementDetailPageResponse response = settlementService.getSellerSettlementDetails(
			sellerId,
			settlementId,
			0,
			20
		);

		assertThat(response.settlement().id()).isEqualTo(settlementId);
		assertThat(response.items()).hasSize(1);
		assertThat(response.items().get(0).settlementTargetId()).isEqualTo(target.getId());
		assertThat(response.items().get(0).orderId()).isEqualTo(target.getOrderId());
	}

	@Test
	void 다른_판매자의_정산상세를_조회하면_예외가_발생한다() {
		UUID sellerId = UUID.randomUUID();
		UUID otherSellerId = UUID.randomUUID();
		UUID settlementId = UUID.randomUUID();

		given(settlementRepository.findById(settlementId)).willReturn(Optional.of(settlement(otherSellerId, "2026-03")));

		assertThatThrownBy(() -> settlementService.getSellerSettlementDetails(sellerId, settlementId, 0, 20))
			.isInstanceOf(BusinessException.class)
			.hasMessage(SettlementErrorCode.SETTLEMENT_NOT_FOUND.getMessage());
	}

	private Settlement settlement(UUID sellerId, String settlementMonth) {
		return new Settlement(
			sellerId,
			settlementMonth,
			new BigDecimal("15000"),
			SellerGradeType.BASIC,
			UUID.randomUUID(),
			new BigDecimal("15000"),
			new BigDecimal("900"),
			new BigDecimal("0.0600"),
			new BigDecimal("14100"),
			SettlementStatus.READY
		);
	}

	private void assignId(Object entity) {
		ReflectionTestUtils.setField(entity, "id", UUID.randomUUID());
	}
}
