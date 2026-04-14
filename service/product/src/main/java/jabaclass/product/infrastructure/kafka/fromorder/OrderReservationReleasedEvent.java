package jabaclass.product.infrastructure.kafka.fromorder;

import java.util.UUID;

public record OrderReservationReleasedEvent(
	UUID eventId,
	UUID productUserId
) {
}
