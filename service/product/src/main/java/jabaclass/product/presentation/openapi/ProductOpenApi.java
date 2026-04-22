package jabaclass.product.presentation.openapi;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jabaclass.product.common.exception.ApiResponseDto;
import jabaclass.product.presentation.dto.request.CreateProductRequestDto;
import jabaclass.product.presentation.dto.request.SearchProductRequestDto;
import jabaclass.product.presentation.dto.request.UpdateProductRequestDto;
import jabaclass.product.presentation.dto.response.DeleteProductResponseDto;
import jabaclass.product.presentation.dto.response.ProductResponseDto;
import jabaclass.product.presentation.dto.response.ProductUserResponseDto;
import jabaclass.product.presentation.dto.response.SearchProductResponseDto;

@Tag(name = "Product", description = "상품 API")
public interface ProductOpenApi {

	@Operation(summary = "상품 생성", description = "신규 상품을 생성 합니다.")
	@ApiResponse(
		responseCode = "201",
		description = "상품 등록 성공",
		content = @Content(
			schema = @Schema(implementation = ApiResponseDto.class)
		)
	)
	@CommonErrorResponses
	ResponseEntity<ApiResponseDto<ProductResponseDto>> create(CreateProductRequestDto request, UUID userId);

	@Operation(summary = "상품 수정", description = "상품을 수정 합니다.")
	@ApiResponse(
		responseCode = "200",
		description = "상품 수정 성공",
		content = @Content(
			schema = @Schema(implementation = ApiResponseDto.class)
		)
	)
	@CommonErrorResponses
	ResponseEntity<ApiResponseDto<ProductResponseDto>> change(UpdateProductRequestDto request
		, UUID productId
		, UUID userId);

	@Operation(summary = "상품 삭제", description = "상품을 삭제 합니다.")
	@ApiResponse(
		responseCode = "202",
		description = "상품 삭제 성공",
		content = @Content(
			schema = @Schema(implementation = ApiResponseDto.class)
		)
	)
	@CommonErrorResponses
	ResponseEntity<ApiResponseDto<DeleteProductResponseDto>> delete(UUID productId, UUID userId);

	@Operation(summary = "상품 전체 검색", description = "전체 상품을 검색 합니다.")
	@ApiResponse(
		responseCode = "202",
		description = "상품 검색 성공",
		content = @Content(
			schema = @Schema(implementation = ApiResponseDto.class)
		)
	)
	@CommonErrorResponses
	ResponseEntity<ApiResponseDto<SearchProductResponseDto>> searchAllProduct(SearchProductRequestDto request);

	@Operation(summary = "판매자 본인 상품 검색", description = "판매자 본인의 상품 목록을 검색 합니다.")
	@ApiResponse(
		responseCode = "200",
		description = "내 상품 검색 성공",
		content = @Content(
			schema = @Schema(implementation = ApiResponseDto.class)
		)
	)
	@CommonErrorResponses
	ResponseEntity<ApiResponseDto<SearchProductResponseDto>> searchMyProducts(SearchProductRequestDto request, UUID userId);

	@Operation(summary = "특정 상품 검색", description = "특정 상품을 검색 합니다.")
	@ApiResponse(
		responseCode = "202",
		description = "상품 검색 성공",
		content = @Content(
			schema = @Schema(implementation = ApiResponseDto.class)
		)
	)
	@CommonErrorResponses
	ResponseEntity<ApiResponseDto<ProductResponseDto>> searchProduct(UUID productId);

	@Operation(summary = "예약 유저 조회", description = "예약 유저 조회를 합니다.")
	@ApiResponse(
		responseCode = "200",
		description = "예약 유저 조회 성공",
		content = @Content(
			array = @io.swagger.v3.oas.annotations.media.ArraySchema(
				schema = @Schema(implementation = ProductUserResponseDto.class))
		)
	)
	@CommonErrorResponses
	ResponseEntity<ApiResponseDto<List<ProductUserResponseDto>>> schedulesSelectUser(UUID scheduleId, UUID userId);

}
