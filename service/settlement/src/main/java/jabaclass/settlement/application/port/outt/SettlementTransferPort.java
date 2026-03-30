package jabaclass.settlement.application.port.outt;

import jabaclass.settlement.application.dto.SettlementTransferCommand;
import jabaclass.settlement.application.dto.SettlementTransferResult;

public interface SettlementTransferPort {
	SettlementTransferResult transfer(SettlementTransferCommand command);
}