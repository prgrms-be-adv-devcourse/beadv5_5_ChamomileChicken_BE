package jabaclass.order.order.presentation.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.UUID;

public record CreateOrderRequestDto(
    @NotNull
	UUID productScheduleId,
    @NotNull
	UUID userId,
    @NotNull @Positive
	Integer quantity,
    @NotNull @DecimalMin(value = "0.0", inclusive = true)
	BigDecimal price
) {
}
