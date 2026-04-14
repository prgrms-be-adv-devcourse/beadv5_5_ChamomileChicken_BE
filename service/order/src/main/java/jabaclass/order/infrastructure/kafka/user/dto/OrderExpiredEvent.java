package jabaclass.order.infrastructure.kafka.user.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record OrderExpiredEvent(
	UUID eventId,
	UUID orderId,
	UUID userId,
	BigDecimal depositAmount
) {
}