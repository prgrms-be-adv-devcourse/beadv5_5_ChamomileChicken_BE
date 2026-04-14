package jabaclass.product.infrastructure.kafka.fromorder;

import java.util.UUID;

public record OrderReservationConfirmedEvent(
	UUID eventId,
	UUID productUserId
) {
}
