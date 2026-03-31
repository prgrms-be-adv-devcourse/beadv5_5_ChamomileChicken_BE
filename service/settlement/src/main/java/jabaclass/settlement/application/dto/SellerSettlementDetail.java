package jabaclass.settlement.application.dto;

import java.util.UUID;

public record SellerSettlementDetail(
	UUID sellerId,
	String role,
	boolean accountRegistered,
	boolean accountActive
) {
	public boolean isSeller() {
		return "SELLER".equals(role);
	}

	public boolean hasActiveSettlementAccount() {
		return accountRegistered && accountActive;
	}
}