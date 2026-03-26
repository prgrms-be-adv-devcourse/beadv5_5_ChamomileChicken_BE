package jabaclass.product.presentation.dto.respose;

import java.util.List;

import org.springframework.data.domain.Page;

import io.swagger.v3.oas.annotations.media.Schema;
import jabaclass.product.domain.model.Product;

@Schema(description = "검색 상품 응답")
public record SearchProductResponseDto(
	@Schema(description = "전체 상품 갯수", example = "100")
	Long totalCount,

	@Schema(description = "전체 상품 페이지 수", example = "10")
	int totalPage,

	@Schema(description = "현재 페이지 번호", example = "1")
	int thisPage,

	@Schema(description = "검색된 상품")
	List<ProductResponseDto> content
) {
	public static SearchProductResponseDto from(Page<Product> product, List<ProductResponseDto> content) {
		return new SearchProductResponseDto(
			product.getTotalElements(),
			product.getTotalPages(),
			product.getNumber(),
			content
		);
	}
}
