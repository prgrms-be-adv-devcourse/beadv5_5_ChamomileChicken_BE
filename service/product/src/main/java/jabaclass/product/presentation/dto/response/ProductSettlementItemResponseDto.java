package jabaclass.product.presentation.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

import jabaclass.product.domain.model.Product;

public record ProductSettlementItemResponseDto(
	UUID productId,
	UUID sellerId,
	BigDecimal productPrice,
	String productStatus
) {
	public static ProductSettlementItemResponseDto from(Product product) {
		return new ProductSettlementItemResponseDto(
			product.getId(),
			product.getSellerId(),
			product.getPrice(),
			product.getStatus().name()
		);
	}
}
