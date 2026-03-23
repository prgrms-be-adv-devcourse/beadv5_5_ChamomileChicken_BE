package jabaclass.order.presentation.dto.response;

import jabaclass.order.domain.model.Order;
import jabaclass.order.domain.model.OrderStatus;

import java.math.BigDecimal;
import java.util.UUID;

public record OrderResponseDto(
    UUID id,
    UUID productScheduleId,
    UUID userId,
    Integer quantity,
    BigDecimal price,
    OrderStatus status
) {

    public static OrderResponseDto from(Order order) {
        return new OrderResponseDto(
            order.getId(),
            order.getProductScheduleId(),
            order.getUserId(),
            order.getQuantity(),
            order.getPrice(),
            order.getStatus()
        );
    }
}
