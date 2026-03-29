package jabaclass.order.order.presentation.dto.request;

import java.math.BigDecimal;
import java.util.UUID;

import jabaclass.order.order.domain.model.PaymentResultStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

public record UpdateOrderPaymentStatusRequestDto(
    @NotNull
    UUID paymentId,
    @NotNull
    PaymentResultStatus paymentStatus,
    @NotNull
    @DecimalMin(value = "0.0", inclusive = true)
    BigDecimal depositAmount
) {
}
