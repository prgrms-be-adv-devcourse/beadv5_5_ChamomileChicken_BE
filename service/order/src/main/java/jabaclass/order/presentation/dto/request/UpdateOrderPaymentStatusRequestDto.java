package jabaclass.order.presentation.dto.request;

import java.math.BigDecimal;

import jabaclass.order.domain.model.PaymentResultStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateOrderPaymentStatusRequestDto(
    @NotNull
    PaymentResultStatus paymentStatus,
    BigDecimal depositAmount
) {
}