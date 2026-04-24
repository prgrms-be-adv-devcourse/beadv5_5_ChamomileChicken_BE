package jabaclass.admin.settlement.domain.dto;

import java.util.UUID;

public record SettlementSearchCondition(
	String status,
	UUID sellerId,
	String settlementMonth
) {
}