package jabaclass.admin.product.presentation.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import jabaclass.admin.product.domain.model.Product;
import jabaclass.admin.product.domain.model.ProductStatus;

public record ProductAdminResponseDto(
	UUID id,
	UUID sellerId,
	String title,
	int maxCapacity,
	BigDecimal price,
	ProductStatus status,
	LocalDateTime createdAt
) {
	public static ProductAdminResponseDto from(Product product) {
		return new ProductAdminResponseDto(
			product.getId(),
			product.getSellerId(),
			product.getTitle(),
			product.getMaxCapacity(),
			product.getPrice(),
			product.getStatus(),
			product.getCreatedAt()
		);
	}
}
