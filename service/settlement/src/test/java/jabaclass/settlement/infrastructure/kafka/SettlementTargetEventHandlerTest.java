package jabaclass.settlement.infrastructure.kafka;

import java.time.LocalDateTime;

import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator.ReplaceUnderscores;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@SuppressWarnings("NonAsciiCharacters")
@DisplayNameGeneration(ReplaceUnderscores.class)
class SettlementTargetEventHandlerTest {

	private final SettlementTargetEventHandler handler = new SettlementTargetEventHandler(null);

	@Test
	void 월말_발생_이벤트가_다음달_1일_1시_전까지_수신되면_발생월_정산에_반영한다() {
		String settlementMonth = handler.resolveSettlementMonth(
			LocalDateTime.of(2026, 4, 30, 23, 59, 55),
			LocalDateTime.of(2026, 5, 1, 0, 59, 59)
		);

		assertThat(settlementMonth).isEqualTo("2026-04");
	}

	@Test
	void 월말_발생_이벤트가_다음달_1일_1시_이후_수신되면_수신월_정산에_반영한다() {
		String settlementMonth = handler.resolveSettlementMonth(
			LocalDateTime.of(2026, 4, 30, 23, 59, 55),
			LocalDateTime.of(2026, 5, 1, 1, 0)
		);

		assertThat(settlementMonth).isEqualTo("2026-05");
	}

	@Test
	void 미래_발생_이벤트는_수신월_정산에_반영한다() {
		String settlementMonth = handler.resolveSettlementMonth(
			LocalDateTime.of(2026, 5, 2, 0, 0),
			LocalDateTime.of(2026, 5, 1, 0, 0)
		);

		assertThat(settlementMonth).isEqualTo("2026-05");
	}
}
