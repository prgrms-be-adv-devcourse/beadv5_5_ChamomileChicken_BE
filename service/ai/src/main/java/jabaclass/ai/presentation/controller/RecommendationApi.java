package jabaclass.ai.presentation.controller;

import java.util.UUID;

import org.springframework.http.ResponseEntity;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jabaclass.ai.common.auth.CurrentUser;
import jabaclass.ai.presentation.dto.response.RecommendationResponseDto;

@Tag(name = "Recommendation", description = "추천 API")
public interface RecommendationApi {
	@Operation(
		summary = "추천 조회",
		description = """
			사용자 맞춤 추천을 조회합니다.

			- Redis 캐시 조회
			- 사용자 벡터 기반 추천
			- GPT 추천 이유 생성
			"""
	)
	ResponseEntity<RecommendationResponseDto> recommend(@CurrentUser UUID userId);
}
