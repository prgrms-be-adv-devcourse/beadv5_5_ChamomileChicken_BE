package jabaclass.admin.review.presentation.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

import jabaclass.admin.review.domain.model.Review;

public record ReviewAdminResponseDto(
	UUID id,
	UUID productId,
	UUID userId,
	String userEmail,
	int rating,
	String content,
	LocalDateTime createdAt
) {
	public static ReviewAdminResponseDto from(Review review, String userEmail) {
		return new ReviewAdminResponseDto(
			review.getId(),
			review.getProductId(),
			review.getUserId(),
			userEmail,
			review.getRating(),
			review.getContent(),
			review.getCreatedAt()
		);
	}
}
