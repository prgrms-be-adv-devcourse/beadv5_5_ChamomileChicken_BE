package jabaclass.ai.application.usecase;

import java.util.UUID;

import jabaclass.ai.presentation.dto.response.RecommendationResponseDto;

public interface RecommendationUseCase {
	RecommendationResponseDto recommend(UUID userId);
}
