package jabaclass.admin.settlement.application.service;

import static org.assertj.core.api.Assertions.assertThat;
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
	private Settlement settlement;

	@BeforeEach
	void setUp() {
		settlementId = UUID.randomUUID();
		settlement = Settlement.builder()
			.sellerId(UUID.randomUUID())
			.settlementMonth("2025-04")
			.originalAmount(new BigDecimal("1000000"))
			.feeAmount(new BigDecimal("30000"))
			.feeRate(new BigDecimal("0.0300"))
			.settlementAmount(new BigDecimal("970000"))
			.status(SettlementStatus.SENT)
			.build();
		ReflectionTestUtils.setField(settlement, "id", settlementId);
	}

	@Test
	void 전체_정산_목록을_조회한다() {
		// given
		Pageable pageable = PageRequest.of(0, 10);
		given(settlementAdminRepository.findAll(pageable))
			.willReturn(new PageImpl<>(List.of(settlement)));

		// when
		Page<SettlementAdminResponseDto> result = settlementAdminService.getSettlements(pageable);

		// then
		assertThat(result.getContent()).hasSize(1);
		assertThat(result.getContent().get(0).id()).isEqualTo(settlementId);
		assertThat(result.getContent().get(0).settlementMonth()).isEqualTo("2025-04");
		assertThat(result.getContent().get(0).status()).isEqualTo(SettlementStatus.SENT);
		then(settlementAdminRepository).should(times(1)).findAll(pageable);
	}
}
