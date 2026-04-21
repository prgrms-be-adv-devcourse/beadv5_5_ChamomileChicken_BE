package jabaclass.product.application.usecase;

import java.util.List;
import java.util.UUID;

import jabaclass.product.presentation.dto.request.ReviewRequestDto;
import jabaclass.product.presentation.dto.response.ReviewResponseDto;

public interface ReviewUseCase {

	ReviewResponseDto createReview(ReviewRequestDto review, UUID productId, UUID userId);

	ReviewResponseDto updateReview(ReviewRequestDto review, UUID reivewId, UUID userId);

	void deleteReview(UUID reviewId, UUID userId);

	List<ReviewResponseDto> userReview(UUID userId);

	List<ReviewResponseDto> productReview(UUID productId);

	ReviewResponseDto review(UUID reivewId);

}
