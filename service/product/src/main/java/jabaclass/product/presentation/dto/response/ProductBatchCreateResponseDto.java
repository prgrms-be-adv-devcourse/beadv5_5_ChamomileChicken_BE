package jabaclass.product.presentation.dto.response;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "상품 일괄 생성 응답")
public record ProductBatchCreateResponseDto(
	@Schema(description = "생성된 상품 수", example = "3")
	int createdCount,

	@Schema(description = "생성된 상품 목록")
	List<ProductResponseDto> products
) {
	public static ProductBatchCreateResponseDto of(List<ProductResponseDto> products) {
		return new ProductBatchCreateResponseDto(products.size(), products);
	}
}
