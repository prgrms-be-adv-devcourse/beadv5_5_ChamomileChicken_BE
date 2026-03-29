package jabaclass.user.deposit.presentation.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

public record DepositChargeResponseDto(
        UUID depositPaymentId,
        BigDecimal chargeAmount,
        BigDecimal balance,
        boolean success
) {
}
