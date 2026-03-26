package jabaclass.order.order.application.client;

import java.util.UUID;

import jabaclass.order.order.infrastructure.client.product.dto.ProductReservationResponseDto;

public interface ProductClient {

    ProductReservationResponseDto reserve(UUID productScheduleId, Integer quantity);

    void release(UUID productScheduleId, Integer quantity);
}
