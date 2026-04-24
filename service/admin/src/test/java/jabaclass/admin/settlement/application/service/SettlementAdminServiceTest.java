package jabaclass.admin.settlement.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator.ReplaceUnderscores;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import jabaclass.admin.settlement.domain.dto.SettlementSearchCondition;
import jabaclass.admin.settlement.domain.model.Settlement;
import jabaclass.admin.settlement.domain.model.SettlementStatus;
import jabaclass.admin.settlement.domain.repository.SettlementAdminRepository;
import jabaclass.admin.settlement.presentation.dto.response.SettlementAdminResponseDto;

@SuppressWarnings("NonAsciiCharacters")
@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(ReplaceUnderscores.class)
class SettlementAdminServiceTest {

	@Mock
	private SettlementAdminRepository settlementAdminRepository;

	@InjectMocks
	private SettlementAdminService settlementAdminService;

	private UUID settlementId;
	private UUID sellerId;
	private Settlement settlement;

	@BeforeEach
	void setUp() {
		settlementId = UUID.randomUUID();
		sellerId = UUID.randomUUID();
		settlement = new Settlement();
		ReflectionTestUtils.setField(settlement, "id", settlementId);
		ReflectionTestUtils.setField(settlement, "sellerId", sellerId);
		ReflectionTestUtils.setField(settlement, "settlementMonth", "2025-04");
		ReflectionTestUtils.setField(settlement, "originalAmount", new BigDecimal("1000000"));
		ReflectionTestUtils.setField(settlement, "feeAmount", new BigDecimal("30000"));
		ReflectionTestUtils.setField(settlement, "feeRate", new BigDecimal("0.0300"));
		ReflectionTestUtils.setField(settlement, "settlementAmount", new BigDecimal("970000"));
		ReflectionTestUtils.setField(settlement, "status", SettlementStatus.READY);
	}

	@Test
	void 조건_없으면_전체_조회_성공() {
		// given
		Pageable pageable = PageRequest.of(0, 10);
		SettlementSearchCondition condition = new SettlementSearchCondition(null, null, null);
		given(settlementAdminRepository.findAll(any(SettlementSearchCondition.class), eq(pageable)))
			.willReturn(new PageImpl<>(List.of(settlement)));

		// when
		Page<SettlementAdminResponseDto> result = settlementAdminService.getSettlements(pageable, condition);

		// then
		assertThat(result.getContent()).hasSize(1);
		assertThat(result.getContent().get(0).id()).isEqualTo(settlementId);
		assertThat(result.getContent().get(0).settlementMonth()).isEqualTo("2025-04");
		assertThat(result.getContent().get(0).status()).isEqualTo(SettlementStatus.READY);
		then(settlementAdminRepository).should(times(1)).findAll(any(SettlementSearchCondition.class), eq(pageable));
	}

	@Test
	void 상태_READY_필터_조회_성공() {
		// given
		Pageable pageable = PageRequest.of(0, 10);
		SettlementSearchCondition condition = new SettlementSearchCondition("READY", null, null);
		given(settlementAdminRepository.findAll(any(SettlementSearchCondition.class), eq(pageable)))
			.willReturn(new PageImpl<>(List.of(settlement)));

		// when
		Page<SettlementAdminResponseDto> result = settlementAdminService.getSettlements(pageable, condition);

		// then
		assertThat(result.getContent()).hasSize(1);
		assertThat(result.getContent().get(0).status()).isEqualTo(SettlementStatus.READY);
		then(settlementAdminRepository).should(times(1)).findAll(any(SettlementSearchCondition.class), eq(pageable));
	}

	@Test
	void 판매자ID_필터_조회_성공() {
		// given
		Pageable pageable = PageRequest.of(0, 10);
		SettlementSearchCondition condition = new SettlementSearchCondition(null, sellerId, null);
		given(settlementAdminRepository.findAll(any(SettlementSearchCondition.class), eq(pageable)))
			.willReturn(new PageImpl<>(List.of(settlement)));

		// when
		Page<SettlementAdminResponseDto> result = settlementAdminService.getSettlements(pageable, condition);

		// then
		assertThat(result.getContent()).hasSize(1);
		assertThat(result.getContent().get(0).sellerId()).isEqualTo(sellerId);
		then(settlementAdminRepository).should(times(1)).findAll(any(SettlementSearchCondition.class), eq(pageable));
	}

	@Test
	void settlementMonth_필터_조회_성공() {
		// given
		Pageable pageable = PageRequest.of(0, 10);
		SettlementSearchCondition condition = new SettlementSearchCondition(null, null, "2025-04");
		given(settlementAdminRepository.findAll(any(SettlementSearchCondition.class), eq(pageable)))
			.willReturn(new PageImpl<>(List.of(settlement)));

		// when
		Page<SettlementAdminResponseDto> result = settlementAdminService.getSettlements(pageable, condition);

		// then
		assertThat(result.getContent()).hasSize(1);
		assertThat(result.getContent().get(0).settlementMonth()).isEqualTo("2025-04");
		then(settlementAdminRepository).should(times(1)).findAll(any(SettlementSearchCondition.class), eq(pageable));
	}
}
