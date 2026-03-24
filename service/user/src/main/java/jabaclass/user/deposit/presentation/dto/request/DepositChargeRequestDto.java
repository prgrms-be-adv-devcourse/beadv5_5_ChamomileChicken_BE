package jabaclass.user.deposit.presentation.dto.request;

import java.math.BigDecimal;

public record DepositChargeRequestDto(
	String paymentMethod,
	BigDecimal chargeAmount
) {
}
