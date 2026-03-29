package jabaclass.order.order.infrastructure.client.product.dto;

import java.util.UUID;

public record ProductReservationReleaseRequestDto(
    UUID productScheduleId,
    UUID userId,
    String status,
    UUID productUserId,
    Integer quantity
) {
}
