package jabaclass.product.infrastructure.kafka.order;

import java.util.UUID;

public record OrderReservationConfirmedEvent(
	UUID eventId,
	UUID orderId,
	UUID productUserId
) {
}
