package jabaclass.product.infrastructure.kafka.order;

import java.util.UUID;

public record OrderReservationReleasedEvent(
	UUID eventId,
	UUID productUserId
) {
}
