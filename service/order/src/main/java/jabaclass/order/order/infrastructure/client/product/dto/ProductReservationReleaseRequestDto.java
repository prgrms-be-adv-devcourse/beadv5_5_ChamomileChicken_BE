package jabaclass.order.order.infrastructure.client.product.dto;

import java.util.UUID;

public record ProductReservationReleaseRequestDto(
    UUID productScheduleId,
    UUID userId,
    ProductReservationStatus status,
    UUID productUserId,
    Integer quantity
) {
}
