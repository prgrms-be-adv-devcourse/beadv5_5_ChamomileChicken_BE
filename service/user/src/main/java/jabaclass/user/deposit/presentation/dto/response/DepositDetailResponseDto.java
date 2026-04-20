package jabaclass.user.deposit.presentation.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

import jabaclass.user.deposit.domain.DepositType;

public record DepositDetailResponseDto(
	UUID depositHistoryId,
	UUID userId,
	UUID paymentId,
	DepositType type,
	BigDecimal amount
) {
}
