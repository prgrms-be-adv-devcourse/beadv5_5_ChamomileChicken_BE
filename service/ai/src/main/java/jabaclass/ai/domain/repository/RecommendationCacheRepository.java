package jabaclass.ai.domain.repository;

import java.util.UUID;

import jabaclass.ai.presentation.dto.response.RecommendationResponseDto;

// 추천 결과를 Redis에 캐싱해서 빠르게 응답
public interface RecommendationCacheRepository {

	RecommendationResponseDto get(UUID userId);

	void save(UUID userId, RecommendationResponseDto response);
}
