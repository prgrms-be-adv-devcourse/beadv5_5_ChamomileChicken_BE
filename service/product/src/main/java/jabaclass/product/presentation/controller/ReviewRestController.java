package jabaclass.product.presentation.controller;

import java.util.List;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jabaclass.product.application.usecase.ReviewUseCase;
import jabaclass.product.common.auth.CurrentUser;
import jabaclass.product.common.exception.ApiResponseDto;
import jabaclass.product.presentation.dto.request.ReviewRequestDto;
import jabaclass.product.presentation.dto.response.ReviewResponseDto;
import jabaclass.product.presentation.openapi.ReviewOpenApi;

@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
@Slf4j
public class ReviewRestController implements ReviewOpenApi {

	private final ReviewUseCase reviewUseCase;

	@Override
	@PostMapping("/{productId}/reviews")
	public ResponseEntity<ApiResponseDto<ReviewResponseDto>> create(@RequestBody ReviewRequestDto request,
		@PathVariable UUID productId,
		@CurrentUser UUID userId) {
		ReviewResponseDto response = reviewUseCase.createReview(request, productId, userId);

		return ResponseEntity.status(HttpStatus.CREATED)
			.body(ApiResponseDto.success(HttpStatus.CREATED, "성공적으로 등록 되었습니다.", response));
	}

	@Override
	@PutMapping("/{productId}/reviews/{reviewId}")
	public ResponseEntity<ApiResponseDto<ReviewResponseDto>> update(@RequestBody ReviewRequestDto request,
		@PathVariable UUID reviewId,
		@CurrentUser UUID userId) {
		ReviewResponseDto response = reviewUseCase.updateReview(request, reviewId, userId);

		return ResponseEntity.ok()
			.body(ApiResponseDto.success(HttpStatus.OK, "성공적으로 수정 되었습니다.", response));
	}

	@Override
	@DeleteMapping("/{productId}/reviews/{reviewId}")
	public ResponseEntity<ApiResponseDto<UUID>> delete(@PathVariable UUID reviewId, @CurrentUser UUID userId) {
		reviewUseCase.deleteReview(reviewId, userId);

		return ResponseEntity.ok()
			.body(ApiResponseDto.success(HttpStatus.OK, "성공적으로 삭제 되었습니다.", null));
	}

	@Override
	@GetMapping("/me/reviews")
	public ResponseEntity<ApiResponseDto<List<ReviewResponseDto>>> userReview(@CurrentUser UUID userId) {
		List<ReviewResponseDto> response = reviewUseCase.userReview(userId);

		return ResponseEntity.ok()
			.body(ApiResponseDto.success(HttpStatus.OK, "성공적으로 검색 되었습니다.", response));
	}

	@Override
	@GetMapping("/{productId}/reviewList")
	public ResponseEntity<ApiResponseDto<List<ReviewResponseDto>>> productReview(@PathVariable UUID productId) {
		List<ReviewResponseDto> response = reviewUseCase.productReview(productId);

		return ResponseEntity.ok()
			.body(ApiResponseDto.success(HttpStatus.OK, "성공적으로 검색 되었습니다.", response));
	}

	@Override
	@GetMapping("/{productId}/reviews/{reviewId}")
	public ResponseEntity<ApiResponseDto<ReviewResponseDto>> review(@PathVariable UUID reviewId) {
		ReviewResponseDto response = reviewUseCase.review(reviewId);

		return ResponseEntity.ok()
			.body(ApiResponseDto.success(HttpStatus.OK, "성공적으로 검색 되었습니다.", response));
	}
}
