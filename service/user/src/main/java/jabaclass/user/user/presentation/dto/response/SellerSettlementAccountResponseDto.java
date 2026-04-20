package jabaclass.user.user.presentation.dto.response;

import java.util.UUID;

import jabaclass.user.user.domain.model.SellerSettlementAccount;

public record SellerSettlementAccountResponseDto(
	UUID sellerId,
	String bankCode,
	String accountNumber,
	String accountHolder,
	boolean active
) {
	public static SellerSettlementAccountResponseDto from(SellerSettlementAccount account) {
		return new SellerSettlementAccountResponseDto(
			account.getUserId(),
			account.getBankCode(),
			account.getAccountNumber(),
			account.getAccountHolder(),
			account.isActive()
		);
	}
}
