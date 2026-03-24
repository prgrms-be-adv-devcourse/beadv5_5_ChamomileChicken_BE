package jabaclass.order.order.presentation.dto.request;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;

public record CancelOrderRequestDto(
    @NotNull
    UUID userId
) {
}
