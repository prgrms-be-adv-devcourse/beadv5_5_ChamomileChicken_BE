package jabaclass.order.order.presentation.dto.response;

import jabaclass.order.order.domain.model.Order;
import jabaclass.order.order.domain.model.OrderStatus;

import java.math.BigDecimal;
import java.util.UUID;

public record OrderResponseDto(
    UUID id,
    UUID productScheduleId,
    UUID buyerId,
    Integer quantity,
    BigDecimal totalAmount,
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
