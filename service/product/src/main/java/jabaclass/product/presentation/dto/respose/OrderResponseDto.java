package jabaclass.product.presentation.dto.respose;

import java.math.BigDecimal;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;
import jabaclass.product.domain.model.Product;

@Schema(description = "주문 API 통신")
public record OrderResponseDto(
	BigDecimal price,

	@Schema(description = "예약 인원", example = "2")
	int quantity,

	@Schema(description = "상품 소유자 Id", example = "550e8400-e29b-41d4-a716-446655440002", nullable = true)
	UUID productUserId,

	@Schema(description = "상품 예약 가능 여부", example = "true")
	boolean valid
) {
	public static OrderResponseDto from(Product product, int quantity, boolean valid) {
		return new OrderResponseDto(
			product.getPrice(),
			quantity,
			valid ? product.getSellerId() : null,
			valid
		);
	}
}
