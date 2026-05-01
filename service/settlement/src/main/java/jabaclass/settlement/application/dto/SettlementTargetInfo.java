package jabaclass.settlement.application.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import jabaclass.settlement.domain.model.settlement.SettlementTargetType;

public record SettlementTargetInfo(
	UUID id,
	String settlementMonth,
	UUID sellerId,
	UUID orderId,
	UUID paymentId,
	UUID refundId,
	UUID productId,
	SettlementTargetType targetType,
	BigDecimal settlementBaseAmount,
	LocalDateTime occurredAt
) {}
