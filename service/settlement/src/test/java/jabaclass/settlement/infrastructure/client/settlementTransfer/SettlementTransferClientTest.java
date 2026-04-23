package jabaclass.settlement.infrastructure.client.settlementTransfer;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import jabaclass.settlement.application.dto.SettlementTransferCommand;
import jabaclass.settlement.application.dto.SettlementTransferResult;

@SuppressWarnings("NonAsciiCharacters")
class SettlementTransferClientTest {

	private final SettlementTransferClient settlementTransferClient = new SettlementTransferClient();

	@Test
	void 같은_settlementId로_요청하면_기존_송금_결과를_반환한다() {
		// given
		UUID settlementId = UUID.randomUUID();
		SettlementTransferCommand failedCommand = command(settlementId, "12349999", BigDecimal.valueOf(10_000));
		SettlementTransferCommand successCommand = command(settlementId, "12345678", BigDecimal.valueOf(10_000));

		// when
		SettlementTransferResult firstResult = settlementTransferClient.transfer(failedCommand);
		SettlementTransferResult duplicatedResult = settlementTransferClient.transfer(successCommand);

		// then
		assertThat(firstResult.success()).isFalse();
		assertThat(duplicatedResult).isEqualTo(firstResult);
	}

	@Test
	void 서로_다른_settlementId는_각각_송금_결과를_생성한다() {
		// given
		SettlementTransferCommand failedCommand = command(UUID.randomUUID(), "12349999", BigDecimal.valueOf(10_000));
		SettlementTransferCommand successCommand = command(UUID.randomUUID(), "12345678", BigDecimal.valueOf(10_000));

		// when
		SettlementTransferResult failedResult = settlementTransferClient.transfer(failedCommand);
		SettlementTransferResult successResult = settlementTransferClient.transfer(successCommand);

		// then
		assertThat(failedResult.success()).isFalse();
		assertThat(successResult.success()).isTrue();
	}

	private SettlementTransferCommand command(UUID settlementId, String accountNumber, BigDecimal amount) {
		return new SettlementTransferCommand(
			settlementId,
			UUID.randomUUID(),
			"088",
			accountNumber,
			"홍길동",
			amount
		);
	}
}
