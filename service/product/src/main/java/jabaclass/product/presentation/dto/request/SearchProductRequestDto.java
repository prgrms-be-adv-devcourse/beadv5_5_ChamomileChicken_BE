package jabaclass.product.presentation.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jabaclass.product.domain.model.status.ProductStatus;

@Schema(description = "상품 검색")
public record SearchProductRequestDto(
	@Schema(description = "검색어", example = "향수")
	String keyword,

	@Schema(description = "현재 페이지", example = "0")
	int thisPage,

	@Schema(description = "한 페이지 상품 갯수", example = "10")
	int pageSize,

	@Schema(description = "공개 상태", example = "ENABLE")
	ProductStatus status
) {

}
