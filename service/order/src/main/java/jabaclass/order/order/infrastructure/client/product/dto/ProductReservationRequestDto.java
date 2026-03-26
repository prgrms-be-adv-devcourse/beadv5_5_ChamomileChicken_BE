package jabaclass.order.order.infrastructure.client.product.dto;

import java.util.UUID;

public record ProductReservationRequestDto(
    UUID productScheduleId,
    Integer quantity
) {
}
