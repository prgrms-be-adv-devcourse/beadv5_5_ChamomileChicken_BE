package jabaclass.ai.application.dto;

import java.math.BigDecimal;
import java.util.UUID;

// GPT 입력에 사용되는 추천 후보 클래스 DTO
public record CandidateClassDto(
	UUID productId,
	String title,
	String description,
	BigDecimal price,
	String roadAddress

) {
}
