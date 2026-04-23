package jabaclass.settlement.application.port.external;

import jabaclass.settlement.application.dto.SettlementTransferCommand;
import jabaclass.settlement.application.dto.SettlementTransferResult;
import jabaclass.settlement.application.dto.SettlementTransferStatusResult;

import java.util.UUID;

public interface SettlementTransferPort {
	SettlementTransferResult transfer(SettlementTransferCommand command);

	SettlementTransferStatusResult getTransferStatus(UUID settlementId);
}
