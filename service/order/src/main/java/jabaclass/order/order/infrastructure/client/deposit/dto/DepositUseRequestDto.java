package jabaclass.order.order.infrastructure.client.deposit.dto;

import java.util.UUID;
import java.math.BigDecimal;

public record DepositUseRequestDto(
    UUID userId,
    BigDecimal amount
) {
}
