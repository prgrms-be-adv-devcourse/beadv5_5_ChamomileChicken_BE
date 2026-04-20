package jabaclass.payment.infrastructure.external.user.dto.request;

import java.math.BigDecimal;
import java.util.UUID;

public record IncreaseDepositRequestDto(
	BigDecimal amount,
	UUID paymentId
) {}