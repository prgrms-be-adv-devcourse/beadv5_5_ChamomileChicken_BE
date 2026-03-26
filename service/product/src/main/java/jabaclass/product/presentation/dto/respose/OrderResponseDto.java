package jabaclass.product.presentation.dto.respose;

import java.math.BigDecimal;

import io.swagger.v3.oas.annotations.media.Schema;
import jabaclass.product.domain.model.Product;

@Schema(description = "주문 API 통신")
public record OrderResponseDto(
	BigDecimal price,

	@Schema(description = "예약 인원", example = "2")
	int quantity,

	@Schema(description = "상품 예약 가능 여부", example = "true")
	boolean valid
) {
	public static OrderResponseDto from(Product product, int quantity, boolean valid) {
		return new OrderResponseDto(
			product.getPrice(),
			quantity,
			valid
		);
	}
}
