package jabaclass.order.presentation.dto.request;

import jabaclass.order.domain.model.PaymentResultStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateOrderPaymentStatusRequestDto(
    @NotNull
    PaymentResultStatus paymentStatus
) {
}
