package jabaclass.settlement.application.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record SettlementTransferCommand(
	UUID settlementId,
	UUID sellerId,
	String bankCode,
	String accountNumber,
	String accountHolder,
	BigDecimal amount
) {
}