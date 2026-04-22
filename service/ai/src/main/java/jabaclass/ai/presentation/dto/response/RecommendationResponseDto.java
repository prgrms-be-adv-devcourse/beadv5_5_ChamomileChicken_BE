package jabaclass.ai.presentation.dto.response;

import java.util.List;

// 최종 출력 형태
public record RecommendationResponseDto(
	List<RecommendationItemDto> recommendations
) {
}
