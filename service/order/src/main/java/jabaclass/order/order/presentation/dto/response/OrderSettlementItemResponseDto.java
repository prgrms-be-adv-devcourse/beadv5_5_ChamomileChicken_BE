package jabaclass.order.order.presentation.dto.response;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

import jabaclass.order.order.domain.model.Order;

public record OrderSettlementItemResponseDto(
    UUID orderId,
    UUID productScheduleId,
    UUID buyerId,
    UUID participantUserId,
    Integer quantity,
    BigDecimal unitPrice,
    BigDecimal orderPrice,
    String orderStatus
) {

    public static OrderSettlementItemResponseDto from(Order order) {
        BigDecimal unitPrice = order.getQuantity() == null || order.getQuantity() == 0
            ? BigDecimal.ZERO
            : order.getPrice().divide(BigDecimal.valueOf(order.getQuantity()), 2, RoundingMode.DOWN);

        return new OrderSettlementItemResponseDto(
            order.getId(),
            order.getProductScheduleId(),
            order.getUserId(),
            order.getProductUserId(),
            order.getQuantity(),
            unitPrice,
            order.getPrice(),
            order.getStatus().name()
        );
    }
}
