package jabaclass.settlement.infrastructure.client.settlementTransfer;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

import jabaclass.settlement.application.dto.SettlementTransferCheckStatus;
import jabaclass.settlement.application.dto.SettlementTransferCommand;
import jabaclass.settlement.application.dto.SettlementTransferResult;
import jabaclass.settlement.application.dto.SettlementTransferStatusResult;
import jabaclass.settlement.application.port.external.SettlementTransferPort;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class SettlementTransferClient implements SettlementTransferPort {

	private static final String FAIL_ACCOUNT_SUFFIX = "9999";

	private final Map<UUID, SettlementTransferResult> transferResults = new ConcurrentHashMap<>();

	@Override
	public SettlementTransferResult transfer(SettlementTransferCommand command) {
		UUID idempotencyKey = command.settlementId();
		SettlementTransferResult result = transferResults.computeIfAbsent(
			idempotencyKey,
			ignored -> executeFakeTransfer(command)
		);

		log.info(
			"[SETTLEMENT_TRANSFER][FAKE_EXTERNAL] idempotencyKey={}, settlementId={}, sellerId={}, amount={}, success={}",
			idempotencyKey,
			command.settlementId(),
			command.sellerId(),
			command.amount(),
			result.success()
		);

		return result;
	}

	@Override
	public SettlementTransferStatusResult getTransferStatus(UUID settlementId) {
		SettlementTransferResult result = transferResults.get(settlementId);
		if (result == null) {
			return SettlementTransferStatusResult.notFound();
		}

		if (result.success()) {
			return SettlementTransferStatusResult.sent();
		}

		return SettlementTransferStatusResult.failed(result.message());
	}

	private SettlementTransferResult executeFakeTransfer(SettlementTransferCommand command) {
		if (command.amount() == null || command.amount().compareTo(BigDecimal.ZERO) <= 0) {
			return SettlementTransferResult.fail("송금 금액이 0 이하입니다.");
		}

		if (command.accountNumber() != null && command.accountNumber().endsWith(FAIL_ACCOUNT_SUFFIX)) {
			return SettlementTransferResult.fail("외부 송금사 테스트 실패 계좌입니다.");
		}

		return SettlementTransferResult.ok();
	}
}
