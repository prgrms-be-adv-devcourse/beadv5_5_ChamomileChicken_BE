package jabaclass.order.application.port.external;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import jabaclass.order.infrastructure.client.product.dto.ProductReservationResponseDto;

public interface ProductPort {

    ProductReservationResponseDto reserve(UUID productScheduleId, UUID userId, Integer quantity, BigDecimal expectedPrice);

    LocalDate getScheduleStartDate(UUID productScheduleId);
}
