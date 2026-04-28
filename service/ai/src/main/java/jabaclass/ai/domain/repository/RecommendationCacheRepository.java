package jabaclass.ai.domain.repository;

import java.util.UUID;

import jabaclass.ai.presentation.dto.response.RecommendationResponseDto;

public interface RecommendationCacheRepository {

	RecommendationResponseDto get(UUID userId);

	void save(UUID userId, RecommendationResponseDto response);

	void delete(UUID userId);

	void deleteAll();
}
