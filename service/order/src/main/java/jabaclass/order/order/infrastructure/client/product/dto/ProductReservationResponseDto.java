package jabaclass.order.order.infrastructure.client.product.dto;

import java.math.BigDecimal;

public record ProductReservationResponseDto(
    BigDecimal price,
    Integer quantity,
    boolean valid
) {
}
