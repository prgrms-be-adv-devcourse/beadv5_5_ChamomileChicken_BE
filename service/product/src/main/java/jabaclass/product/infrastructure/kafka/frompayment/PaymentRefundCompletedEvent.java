package jabaclass.product.infrastructure.kafka.frompayment;

import java.util.UUID;

public record PaymentRefundCompletedEvent(
	UUID productUserId
) {
}
