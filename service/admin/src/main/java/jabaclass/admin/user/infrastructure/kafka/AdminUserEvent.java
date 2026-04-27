package jabaclass.admin.user.infrastructure.kafka;

import java.time.LocalDateTime;
import java.util.UUID;

public record AdminUserEvent(
	UUID eventId,
	String type,
	UUID sellerId,
	LocalDateTime approvedAt
) {

	public static AdminUserEvent sellerApproved(UUID sellerId, LocalDateTime approvedAt) {
		return new AdminUserEvent(
			UUID.randomUUID(),
			"SELLER_APPROVED",
			sellerId,
			approvedAt
		);
	}
}
