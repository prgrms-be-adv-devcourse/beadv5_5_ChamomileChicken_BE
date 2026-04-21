package jabaclass.settlement.application.usecase;

import java.util.List;
import java.util.UUID;

import jabaclass.settlement.domain.model.settlement.Settlement;
import jabaclass.settlement.presentation.dto.response.SellerSettlementDetailPageResponse;
import jabaclass.settlement.presentation.dto.response.SellerSettlementPageResponse;

public interface SettlementUseCase {
	List<Settlement> getSettlementsByMonth(String settlementMonth);
	List<Settlement> getReadySettlementsByMonth(String settlementMonth);
	Settlement getSettlement(UUID settlementId);
	SellerSettlementPageResponse getSellerSettlements(UUID sellerId, int page, int size);
	SellerSettlementDetailPageResponse getSellerSettlementDetails(UUID sellerId, UUID settlementId, int page, int size);
}
