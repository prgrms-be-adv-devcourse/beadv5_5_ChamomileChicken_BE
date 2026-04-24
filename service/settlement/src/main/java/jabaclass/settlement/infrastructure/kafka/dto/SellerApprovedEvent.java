package jabaclass.settlement.infrastructure.kafka.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record SellerApprovedEvent(
	UUID eventId,
	String type,
	UUID sellerId,
	LocalDateTime approvedAt
) {
}
