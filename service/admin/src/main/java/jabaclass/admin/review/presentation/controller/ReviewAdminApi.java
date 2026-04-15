package jabaclass.admin.review.presentation.controller;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jabaclass.admin.common.dto.ApiResponseDto;

@Tag(name = "Admin - Review", description = "어드민 리뷰 관리 API")
public interface ReviewAdminApi {

	@Operation(summary = "부적절한 리뷰 삭제")
	ResponseEntity<ApiResponseDto<Void>> deleteReview(@PathVariable UUID reviewId);
}
