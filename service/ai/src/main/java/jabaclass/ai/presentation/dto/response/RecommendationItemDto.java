package jabaclass.ai.presentation.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

// 추천 하나의 데이터
public record RecommendationItemDto(
	UUID productId,
	String title,
	String description,
	BigDecimal price,
	String roadAddress,
	String reason
) {
}
