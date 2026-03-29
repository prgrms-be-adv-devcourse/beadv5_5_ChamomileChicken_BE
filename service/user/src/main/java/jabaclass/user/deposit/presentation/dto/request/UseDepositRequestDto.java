package jabaclass.user.deposit.presentation.dto.request;

import java.math.BigDecimal;
import java.util.UUID;

public record UseDepositRequestDto(
	UUID userId,
	BigDecimal depositAmount
) {
}
