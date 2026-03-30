package jabaclass.product.presentation.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;

@Schema(description = "리뷰 생성")
public record ReviewRequestDto(

	@Schema(description = "별점", example = "5")
	@Min(value = 1, message = "예약 가능 인원수를 입력해주세요.")
	int rating,
	
	@Schema(description = "리뷰 내용", example = "정말 좋았어요")
	String content

) {
}
