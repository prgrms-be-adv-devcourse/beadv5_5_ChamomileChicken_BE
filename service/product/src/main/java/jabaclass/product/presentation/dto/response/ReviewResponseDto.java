package jabaclass.product.presentation.dto.response;

import java.time.LocalDateTime;
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
	String content,

	@Schema(description = "작성자명", example = "홍길동")
	String userName,

	@Schema(description = "작성시간")
	LocalDateTime createdAt

) {

	public static ReviewResponseDto from(Review review) {
		return new ReviewResponseDto(
			review.getId(),
			review.getRating(),
			review.getContent(),
			null,
			review.getRegDt()
		);
	}

	public static ReviewResponseDto from(Review review, String userName) {
		return new ReviewResponseDto(
			review.getId(),
			review.getRating(),
			review.getContent(),
			userName,
			review.getRegDt()
		);
	}
}
