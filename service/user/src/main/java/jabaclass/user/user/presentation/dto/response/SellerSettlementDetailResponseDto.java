package jabaclass.user.user.presentation.dto.response;

import java.util.UUID;

public record SellerSettlementDetailResponseDto(
	UUID sellerId,
	String role,
	boolean accountRegistered,
	boolean accountActive
) {
}
