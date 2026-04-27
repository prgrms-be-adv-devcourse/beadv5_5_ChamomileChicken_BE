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
import jabaclass.product.presentation.dto.request.ReviewRequestDto;
import jabaclass.product.presentation.dto.response.ReviewResponseDto;

@Tag(name = "Review", description = "리뷰 API")
public interface ReviewOpenApi {

	@Operation(summary = "리뷰 생성", description = "리뷰를 생성 합니다.")
	@ApiResponse(
		responseCode = "201",
		description = "리뷰 생성 성공",
		content = @Content(
			schema = @Schema(implementation = ApiResponseDto.class)
		)
	)
	@CommonErrorResponses
	ResponseEntity<ApiResponseDto<ReviewResponseDto>> create(ReviewRequestDto request, UUID productId, UUID userId);

	@Operation(summary = "리뷰 수정", description = "리뷰를 수정 합니다.")
	@ApiResponse(
		responseCode = "200",
		description = "리뷰 수정 성공",
		content = @Content(
			schema = @Schema(implementation = ApiResponseDto.class)
		)
	)
	@CommonErrorResponses
	ResponseEntity<ApiResponseDto<ReviewResponseDto>> update(ReviewRequestDto request, UUID reivewId, UUID userId);

	@Operation(summary = "리뷰 삭제", description = "리뷰를 삭제 합니다.")
	@ApiResponse(
		responseCode = "200",
		description = "리뷰 삭제 성공",
		content = @Content(
			schema = @Schema(implementation = ApiResponseDto.class)
		)
	)
	@CommonErrorResponses
	ResponseEntity<ApiResponseDto<UUID>> delete(UUID reivewId, UUID userId);

	@Operation(summary = "개인 리뷰", description = "본인 리뷰 목록을 검색합니다.")
	@ApiResponse(
		responseCode = "200",
		description = "리뷰 검색 성공",
		content = @Content(
			schema = @Schema(implementation = ApiResponseDto.class)
		)
	)
	@CommonErrorResponses
	ResponseEntity<ApiResponseDto<List<ReviewResponseDto>>> userReview(UUID userId);

	@Operation(summary = "상품 리뷰", description = "상품 리뷰 목록을 검색합니다.")
	@ApiResponse(
		responseCode = "200",
		description = "리뷰 검색 성공",
		content = @Content(
			schema = @Schema(implementation = ApiResponseDto.class)
		)
	)
	@CommonErrorResponses
	ResponseEntity<ApiResponseDto<List<ReviewResponseDto>>> productReview(UUID productId);

	@Operation(summary = "상품 단일 리뷰", description = "상품 단일 리뷰를 검색합니다.")
	@ApiResponse(
		responseCode = "200",
		description = "리뷰 검색 성공",
		content = @Content(
			schema = @Schema(implementation = ApiResponseDto.class)
		)
	)
	@CommonErrorResponses
	ResponseEntity<ApiResponseDto<ReviewResponseDto>> review(UUID reviewId);
	
}
