package jabaclass.settlement.domain.repository;

import java.util.List;
import java.util.UUID;

import jabaclass.settlement.domain.model.SettlementTransfer;

public interface SettlementTransferRepository {

	List<SettlementTransfer> saveAll(List<SettlementTransfer> settlementTransfers);
}
