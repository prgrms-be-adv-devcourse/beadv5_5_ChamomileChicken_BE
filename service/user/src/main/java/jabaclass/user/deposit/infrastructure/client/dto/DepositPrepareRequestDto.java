package jabaclass.user.deposit.infrastructure.client.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record DepositPrepareRequestDto(
	UUID userId,
	BigDecimal amount,
	String paymentMethod
) {
}
