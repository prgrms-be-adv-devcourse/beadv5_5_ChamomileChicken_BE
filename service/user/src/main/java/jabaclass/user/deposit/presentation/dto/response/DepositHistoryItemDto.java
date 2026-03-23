package jabaclass.user.deposit.presentation.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

import jabaclass.user.deposit.domain.DepositType;

public record DepositHistoryItemDto(
	UUID depositHistoryId,    // depositHistoryItemDto → depositHistoryId
	UUID userId,
	UUID paymentId,
	DepositType type,
	BigDecimal amount
) {
}
