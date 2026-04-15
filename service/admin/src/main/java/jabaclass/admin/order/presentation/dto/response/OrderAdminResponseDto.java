package jabaclass.admin.order.presentation.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

import jabaclass.admin.order.domain.model.Order;
import jabaclass.admin.order.domain.model.OrderStatus;

public record OrderAdminResponseDto(
	UUID id,
	UUID productScheduleId,
	UUID userId,
	Integer quantity,
	BigDecimal price,
	OrderStatus status
) {
	public static OrderAdminResponseDto from(Order order) {
		return new OrderAdminResponseDto(
			order.getId(),
			order.getProductScheduleId(),
			order.getUserId(),
			order.getQuantity(),
			order.getPrice(),
			order.getStatus()
		);
	}
}
