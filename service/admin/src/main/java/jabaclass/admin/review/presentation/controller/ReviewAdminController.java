package jabaclass.admin.review.presentation.controller;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jabaclass.admin.common.dto.ApiResponseDto;
import jabaclass.admin.review.application.usecase.ReviewAdminUseCase;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/admins/reviews")
@RequiredArgsConstructor
public class ReviewAdminController implements ReviewAdminApi {

	private final ReviewAdminUseCase reviewAdminUseCase;

	@Override
	@DeleteMapping("/{reviewId}")
	public ResponseEntity<ApiResponseDto<Void>> deleteReview(@PathVariable UUID reviewId) {
		reviewAdminUseCase.deleteReview(reviewId);
		return ResponseEntity.ok(
			ApiResponseDto.success(HttpStatus.OK, "리뷰 삭제 완료", null)
		);
	}
}
