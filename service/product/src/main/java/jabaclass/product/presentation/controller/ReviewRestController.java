package jabaclass.product.presentation.controller;

import java.util.List;
import java.util.UUID;

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
import jabaclass.product.common.exception.ApiResponseDto;
import jabaclass.product.presentation.dto.request.ReviewRequestDto;
import jabaclass.product.presentation.dto.respose.ReviewResponseDto;
import jabaclass.product.presentation.openapi.ReviewOpenApi;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
@Slf4j
public class ReviewRestController implements ReviewOpenApi {

	private final ReviewUseCase reviewUseCase;

	@Override
	@PostMapping("/{productId}/reviews")
	public ResponseEntity<ApiResponseDto<ReviewResponseDto>> create(@RequestBody ReviewRequestDto request,
		@PathVariable UUID productId) {
		ReviewResponseDto response = reviewUseCase.createReview(request, productId);

		return ResponseEntity.status(HttpStatus.CREATED)
			.body(ApiResponseDto.success(HttpStatus.CREATED, "성공적으로 등록 되었습니다.", response));
	}

	@Override
	@PutMapping("/{productId}/reviews/{reviewId}")
	public ResponseEntity<ApiResponseDto<ReviewResponseDto>> update(ReviewRequestDto request, UUID reivewId) {
		ReviewResponseDto response = reviewUseCase.updateReview(request, reivewId);

		return ResponseEntity.ok()
			.body(ApiResponseDto.success(HttpStatus.OK, "성공적으로 수정 되었습니다.", response));
	}

	@Override
	@DeleteMapping("/{productId}/reviews/{reviewId}")
	public ResponseEntity<ApiResponseDto<UUID>> delete(UUID reivewId) {
		reviewUseCase.deleteReview(reivewId);

		return ResponseEntity.ok()
			.body(ApiResponseDto.success(HttpStatus.OK, "성공적으로 삭제 되었습니다.", null));
	}

	@Override
	@GetMapping("/{productId}/reviews/user")
	public ResponseEntity<ApiResponseDto<List<ReviewResponseDto>>> userReview() {
		List<ReviewResponseDto> response = reviewUseCase.userReview();

		return ResponseEntity.ok()
			.body(ApiResponseDto.success(HttpStatus.OK, "성공적으로 검색 되었습니다.", response));
	}

	@Override
	@GetMapping("/{productId}/reviews")
	public ResponseEntity<ApiResponseDto<List<ReviewResponseDto>>> productReview(@PathVariable UUID productId) {
		List<ReviewResponseDto> response = reviewUseCase.prodcutReview(productId);

		return ResponseEntity.ok()
			.body(ApiResponseDto.success(HttpStatus.OK, "성공적으로 검색 되었습니다.", response));
	}

	@Override
	@GetMapping("/{productId}/reviews/{reviewId}")
	public ResponseEntity<ApiResponseDto<ReviewResponseDto>> review(UUID reviewId) {
		ReviewResponseDto response = reviewUseCase.reivew(reviewId);

		return ResponseEntity.ok()
			.body(ApiResponseDto.success(HttpStatus.OK, "성공적으로 검색 되었습니다.", response));
	}
}
