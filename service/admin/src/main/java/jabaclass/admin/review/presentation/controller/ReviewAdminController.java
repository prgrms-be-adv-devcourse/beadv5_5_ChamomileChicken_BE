package jabaclass.admin.review.presentation.controller;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jabaclass.admin.common.dto.ApiResponseDto;
import jabaclass.admin.review.application.usecase.ReviewAdminUseCase;
import jabaclass.admin.review.presentation.dto.response.ReviewAdminResponseDto;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/admins/reviews")
@RequiredArgsConstructor
public class ReviewAdminController implements ReviewAdminApi {

	private final ReviewAdminUseCase reviewAdminUseCase;

	@Override
	@GetMapping
	public ResponseEntity<ApiResponseDto<Page<ReviewAdminResponseDto>>> getReviews(Pageable pageable) {
		return ResponseEntity.ok(
			ApiResponseDto.success(HttpStatus.OK, "리뷰 목록 조회 성공", reviewAdminUseCase.getReviews(pageable))
		);
	}

	@Override
	@DeleteMapping("/{reviewId}")
	public ResponseEntity<ApiResponseDto<Void>> deleteReview(@PathVariable UUID reviewId) {
		reviewAdminUseCase.deleteReview(reviewId);
		return ResponseEntity.ok(
			ApiResponseDto.success(HttpStatus.OK, "리뷰 삭제 완료", null)
		);
	}
}
