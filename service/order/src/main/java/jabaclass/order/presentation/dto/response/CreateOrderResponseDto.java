package jabaclass.order.presentation.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

import jabaclass.order.domain.model.Order;
import jabaclass.order.domain.model.OrderStatus;

public record CreateOrderResponseDto(
    UUID id,
    UUID buyerId,
    UUID productId,
    UUID productScheduleId,
    UUID productUserId,
    Integer quantity,
    BigDecimal totalAmount,
    BigDecimal depositAmount,
    BigDecimal paymentAmount,
    OrderStatus status
) {

    public static CreateOrderResponseDto of(Order order, UUID productId) {
        return new CreateOrderResponseDto(
            order.getId(),
            order.getUserId(),
            productId,
            order.getProductScheduleId(),
            order.getProductUserId(),
            order.getQuantity(),
            order.getPrice(),
            order.getDepositAmount(),
            order.getPrice().subtract(order.getDepositAmount()),
            order.getStatus()
        );
    }
}
