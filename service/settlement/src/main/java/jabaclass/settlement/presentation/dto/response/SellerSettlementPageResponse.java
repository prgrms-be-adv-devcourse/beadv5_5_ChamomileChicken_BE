package jabaclass.settlement.presentation.dto.response;

import java.util.List;

public record SellerSettlementPageResponse(
	List<SettlementResponse> items,
	int page,
	int size,
	long totalElements,
	int totalPages,
	boolean hasNext
) {
}
