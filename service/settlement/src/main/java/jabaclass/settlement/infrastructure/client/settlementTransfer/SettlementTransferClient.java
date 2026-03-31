package jabaclass.settlement.infrastructure.client.settlementTransfer;

import org.springframework.stereotype.Component;

import jabaclass.settlement.application.dto.SettlementTransferCommand;
import jabaclass.settlement.application.dto.SettlementTransferResult;
import jabaclass.settlement.application.port.external.SettlementTransferPort;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class SettlementTransferClient implements SettlementTransferPort {

	@Override
	public SettlementTransferResult transfer(SettlementTransferCommand command) {
		log.info(
			"[SETTLEMENT_TRANSFER][LOCAL_SUCCESS] settlementId={}, sellerId={}, amount={}",
			command.settlementId(),
			command.sellerId(),
			command.amount()
		);

		return SettlementTransferResult.ok();
	}
}
