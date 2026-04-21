package jabaclass.product.presentation.openapi;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jabaclass.product.presentation.dto.request.ProductBulkReadRequestDto;
import jabaclass.product.presentation.dto.response.ProductSettlementItemResponseDto;

@Tag(name = "Product Internal", description = "상품 내부 연동 API")
public interface ProductInternalOpenApi {

	@Operation(
		summary = "상품 벌크 조회",
		description = """
			정산 모듈이 상품 ID 목록으로 정산에 필요한 상품 정보를 조회합니다.
			- 판매자 ID
			- 상품 가격
			- 상품 상태
			"""
	)
	@ApiResponse(
		responseCode = "200",
		description = "상품 벌크 조회 성공",
		content = @Content(array = @ArraySchema(schema = @Schema(implementation = ProductSettlementItemResponseDto.class)))
	)
	@CommonErrorResponses
	ResponseEntity<List<ProductSettlementItemResponseDto>> getProductsByIds(
		@RequestBody ProductBulkReadRequestDto requestDto
	);
}
