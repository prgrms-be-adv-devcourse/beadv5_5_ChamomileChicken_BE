package jabaclass.product.infrastructure.kafka;

import java.util.UUID;

// 2026-04-09 수정
public record PaymentRefundCompletedEvent(
	PaymentStatus status,
	UUID productUserId
) {
}
