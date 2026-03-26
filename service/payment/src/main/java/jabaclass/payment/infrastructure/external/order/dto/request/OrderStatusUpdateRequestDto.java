package jabaclass.payment.infrastructure.external.order.dto.request;

import java.util.UUID;

public record OrderStatusUpdateRequestDto(
	UUID paymentId,
	int depositAmount,
	String paymentStatus
) {}
