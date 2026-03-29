package jabaclass.order.order.application.client;

import java.util.UUID;

import jabaclass.order.order.infrastructure.client.product.dto.ProductReservationStatus;
import jabaclass.order.order.infrastructure.client.product.dto.ProductReservationResponseDto;

public interface ProductClient {

    ProductReservationResponseDto reserve(UUID productScheduleId, UUID userId, Integer quantity);

    void updateReservation(UUID productScheduleId, UUID userId, ProductReservationStatus status, UUID productUserId, Integer quantity);
}
