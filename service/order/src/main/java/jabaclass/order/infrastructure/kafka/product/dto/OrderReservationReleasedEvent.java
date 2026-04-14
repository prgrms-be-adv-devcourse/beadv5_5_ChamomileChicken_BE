package jabaclass.order.infrastructure.kafka.product.dto;

import java.util.UUID;

public record OrderReservationReleasedEvent(
	UUID eventId,
	UUID productUserId
) {
}