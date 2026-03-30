package jabaclass.product.application.usecase;

import java.util.List;
import java.util.UUID;

import jabaclass.product.presentation.dto.request.ReviewRequestDto;
import jabaclass.product.presentation.dto.respose.ReviewResponseDto;

public interface ReviewUseCase {

	ReviewResponseDto createReview(ReviewRequestDto review, UUID productId);

	ReviewResponseDto updateReview(ReviewRequestDto review, UUID reivewId);

	void deleteReview(UUID reviewId);

	List<ReviewResponseDto> userReview();

	List<ReviewResponseDto> productReview(UUID productId);

	ReviewResponseDto review(UUID reivewId);

}
