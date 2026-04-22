package jabaclass.admin.order.presentation.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import jabaclass.admin.order.domain.model.Order;
import jabaclass.admin.order.domain.model.OrderStatus;

public record OrderAdminResponseDto(
	UUID id,
	UUID productScheduleId,
	UUID userId,
	UUID sellerId,
	Integer quantity,
	BigDecimal price,
	OrderStatus status,
	LocalDateTime createdAt
) {
	public static OrderAdminResponseDto from(Order order) {
		return new OrderAdminResponseDto(
			order.getId(),
			order.getProductScheduleId(),
			order.getUserId(),
			order.getSellerId(),
			order.getQuantity(),
			order.getPrice(),
			order.getStatus(),
			order.getCreatedAt()
		);
	}
}
