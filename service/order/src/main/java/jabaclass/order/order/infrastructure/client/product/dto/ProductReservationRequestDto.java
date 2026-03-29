package jabaclass.order.order.infrastructure.client.product.dto;

import java.util.UUID;

public record ProductReservationRequestDto(
    UUID productScheduleId,
    UUID userId,
    ProductReservationStatus status,
    Integer quantity
) {
}
