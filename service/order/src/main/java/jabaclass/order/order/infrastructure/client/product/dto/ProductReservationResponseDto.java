package jabaclass.order.order.infrastructure.client.product.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record ProductReservationResponseDto(
    BigDecimal price,
    Integer quantity,
    UUID productUserId,
    boolean valid
) {
}
