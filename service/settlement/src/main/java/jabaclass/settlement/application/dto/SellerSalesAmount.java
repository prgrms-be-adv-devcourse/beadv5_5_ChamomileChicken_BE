package jabaclass.settlement.application.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record SellerSalesAmount(
	UUID sellerId,
	BigDecimal salesAmount
) {
}
