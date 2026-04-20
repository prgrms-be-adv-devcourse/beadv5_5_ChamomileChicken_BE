package jabaclass.settlement.application.usecase;

import java.util.List;
import java.util.UUID;

import jabaclass.settlement.domain.model.Settlement;

public interface SettlementUseCase {
	List<Settlement> getSettlementsByMonth(String settlementMonth);
	List<Settlement> getReadySettlementsByMonth(String settlementMonth);
	Settlement getSettlement(UUID settlementId);
}