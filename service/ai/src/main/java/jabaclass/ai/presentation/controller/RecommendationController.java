package jabaclass.ai.presentation.controller;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jabaclass.ai.application.usecase.RecommendationUseCase;
import jabaclass.ai.common.auth.CurrentUser;
import jabaclass.ai.presentation.dto.response.RecommendationResponseDto;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/recommendations")
public class RecommendationController implements RecommendationApi{

	private final RecommendationUseCase recommendationUseCase;

	@GetMapping
	public ResponseEntity<RecommendationResponseDto> recommend(
		@CurrentUser UUID userId
	) {
		RecommendationResponseDto response =
			recommendationUseCase.recommend(userId);

		return ResponseEntity.ok(response);
	}
}
