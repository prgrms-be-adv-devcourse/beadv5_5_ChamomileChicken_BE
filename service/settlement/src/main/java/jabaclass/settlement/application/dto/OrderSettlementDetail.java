package jabaclass.settlement.application.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record OrderSettlementDetail(
	UUID orderId,
	UUID productScheduleId,
	UUID buyerId,
	UUID participantUserId,
	Integer quantity,
	BigDecimal unitPrice,
	BigDecimal orderPrice,
	String orderStatus
) {
}