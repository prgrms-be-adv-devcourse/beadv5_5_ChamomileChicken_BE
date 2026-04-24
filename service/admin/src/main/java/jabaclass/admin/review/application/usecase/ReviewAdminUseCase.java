package jabaclass.admin.review.application.usecase;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import jabaclass.admin.review.presentation.dto.response.ReviewAdminResponseDto;

public interface ReviewAdminUseCase {

	Page<ReviewAdminResponseDto> getReviews(Pageable pageable);

	void deleteReview(UUID reviewId);

}
