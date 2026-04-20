package jabaclass.payment.infrastructure.external.order.dto.request;

import java.math.BigDecimal;
import java.util.UUID;

import jabaclass.payment.domain.model.PaymentResultStatus;

public record OrderStatusUpdateRequestDto(
	UUID paymentId,
	PaymentResultStatus paymentStatus,
	BigDecimal depositAmount
) {}
