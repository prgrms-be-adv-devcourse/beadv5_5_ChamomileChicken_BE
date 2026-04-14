package jabaclass.order.infrastructure.kafka.product.dto;

import java.util.UUID;

public record OrderReservationConfirmedEvent(
	UUID eventId,
	UUID productUserId
) {
}