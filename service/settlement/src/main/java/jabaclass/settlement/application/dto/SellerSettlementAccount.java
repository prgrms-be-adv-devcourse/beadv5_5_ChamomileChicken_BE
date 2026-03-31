package jabaclass.settlement.application.dto;

import java.util.UUID;

public record SellerSettlementAccount(
	UUID sellerId,
	String bankCode,
	String accountNumber,
	String accountHolder,
	boolean active
) {
	public boolean isTransferable() {
		return active
			&& bankCode != null && !bankCode.isBlank()
			&& accountNumber != null && !accountNumber.isBlank()
			&& accountHolder != null && !accountHolder.isBlank();
	}
}