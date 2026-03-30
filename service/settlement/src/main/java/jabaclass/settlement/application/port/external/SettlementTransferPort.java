package jabaclass.settlement.application.port.external;

import jabaclass.settlement.application.dto.SettlementTransferCommand;
import jabaclass.settlement.application.dto.SettlementTransferResult;

public interface SettlementTransferPort {
	SettlementTransferResult transfer(SettlementTransferCommand command);
}