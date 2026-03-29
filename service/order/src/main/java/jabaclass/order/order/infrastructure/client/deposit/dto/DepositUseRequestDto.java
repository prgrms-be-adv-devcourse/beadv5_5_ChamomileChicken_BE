package jabaclass.order.order.infrastructure.client.deposit.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record DepositUseRequestDto(
	UUID userId,
	BigDecimal depositAmount
) {
}
