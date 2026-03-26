package jabaclass.order.order.presentation.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

import jabaclass.order.order.domain.model.Order;
import jabaclass.order.order.domain.model.OrderStatus;

public record CreateOrderResponseDto(
    UUID id,
    UUID buyerId,
    UUID productId,
    UUID productScheduleId,
    Integer quantity,
    BigDecimal totalAmount,
    BigDecimal depositAmount,
    BigDecimal paymentAmount,
    OrderStatus status
) {

    public static CreateOrderResponseDto of(Order order, UUID productId, BigDecimal depositAmount) {
        return new CreateOrderResponseDto(
            order.getId(),
            order.getUserId(),
            productId,
            order.getProductScheduleId(),
            order.getQuantity(),
            order.getPrice(),
            depositAmount,
            order.getPrice().subtract(depositAmount),
            order.getStatus()
        );
    }
}
