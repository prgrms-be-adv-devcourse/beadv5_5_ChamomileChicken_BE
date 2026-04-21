package jabaclass.product.presentation.dto.response;

import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;
import jabaclass.product.domain.model.Review;

@Schema(description = "리뷰 응답")
public record ReviewResponseDto(

	@Schema(description = "리뷰 Id", example = "11111111-1111-1111-1111-111111111111")
	UUID id,

	@Schema(description = "별점", example = "5")
	int rating,

	@Schema(description = "리뷰 내용", example = "정말 좋았어요")
	String content

) {

	public static ReviewResponseDto from(Review review) {
		return new ReviewResponseDto(
			review.getId(),
			review.getRating(),
			review.getContent()
		);
	}
}
