package jabaclass.settlement.application.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record ProductSettlementDetail(
	UUID productId,
	UUID sellerId,
	BigDecimal productPrice,
	String productStatus
) {
}