package jabaclass.user.deposit.presentation.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

public record DepositMeResponseDto(
	UUID userId,
	BigDecimal balance
) {
}
