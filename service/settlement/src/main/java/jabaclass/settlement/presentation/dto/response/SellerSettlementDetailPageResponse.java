package jabaclass.settlement.presentation.dto.response;

import java.util.List;

public record SellerSettlementDetailPageResponse(
	SettlementResponse settlement,
	List<SellerSettlementDetailItemResponse> items,
	int page,
	int size,
	long totalElements,
	int totalPages,
	boolean hasNext
) {
}
