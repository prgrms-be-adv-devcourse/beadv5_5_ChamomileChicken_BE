package jabaclass.product.presentation.openapi;

import org.springframework.http.ResponseEntity;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jabaclass.product.common.exception.ApiResponseDto;
import jabaclass.product.presentation.dto.request.CreateProductRequestDto;
import jabaclass.product.presentation.dto.respose.CreateProductResponseDto;

@Tag(name = "Product", description = "상품 API")
public interface ProductOpenApi {

	@Operation(summary = "상품 생성", description = "신규 상품을 생성합니다.")
	@ApiResponse(
		responseCode = "201",
		description = "상품 등록 성공",
		content = @Content(
			schema = @Schema(implementation = ApiResponseDto.class)
		)
	)
	@CommonErrorResponses
	ResponseEntity<ApiResponseDto<CreateProductResponseDto>> create(CreateProductRequestDto request);
}
