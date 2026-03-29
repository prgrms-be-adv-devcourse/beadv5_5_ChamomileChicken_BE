package jabaclass.user.common.kafka;

import java.math.BigDecimal;
import java.util.UUID;

public record OrderRefundedEvent(
	UUID orderId,
	UUID userId,
	UUID productScheduleId,
	int quantity,
	BigDecimal depositAmount
) {
}
