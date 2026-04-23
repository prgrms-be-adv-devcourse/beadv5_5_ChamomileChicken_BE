package jabaclass.ai.infrastructure.kafka;

import java.util.UUID;

public record ProductWishlistedEvent(
	UUID eventId,
	UUID userId,
	UUID productId
) {
}
