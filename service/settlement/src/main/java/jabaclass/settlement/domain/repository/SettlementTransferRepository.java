package jabaclass.settlement.domain.repository;

import java.util.List;
import java.util.UUID;

import jabaclass.settlement.domain.model.settlement.SettlementTransfer;

public interface SettlementTransferRepository {

	SettlementTransfer save(SettlementTransfer settlementTransfer);

	List<SettlementTransfer> saveAll(List<SettlementTransfer> settlementTransfers);
}
