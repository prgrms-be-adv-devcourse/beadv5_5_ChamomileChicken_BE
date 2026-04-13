package jabaclass.order.infrastructure.client.product.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record ProductReservationRequestDto(
    UUID productScheduleId,
    UUID userId,
    Integer quantity,
    BigDecimal price
) {
}
