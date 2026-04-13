package jabaclass.order.infrastructure.kafka;

import java.math.BigDecimal;
import java.util.UUID;

public record OrderExpiredEvent(
	UUID orderId,
	UUID userId,
	BigDecimal depositAmount
) {
}
