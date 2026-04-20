package jabaclass.order.infrastructure.client.product.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record ProductReservationResponseDto(
    BigDecimal price,
    int quantity,
    String valid,
    UUID productUserId,
    UUID sellerId
) {
    public boolean isOk() {
        return "OK".equals(valid);
    }
}
