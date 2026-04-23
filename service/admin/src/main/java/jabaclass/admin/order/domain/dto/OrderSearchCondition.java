package jabaclass.admin.order.domain.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record OrderSearchCondition(
	String status,
	UUID sellerId,
	LocalDateTime startDate,
	LocalDateTime endDate
) {
}