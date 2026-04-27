package jabaclass.ai.application.service;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;

import jabaclass.ai.application.dto.CandidateClassDto;
import jabaclass.ai.application.port.external.AiGatewayPort;
import jabaclass.ai.application.usecase.RecommendationUseCase;
import jabaclass.ai.domain.model.UserVector;
import jabaclass.ai.domain.repository.CandidateSearchRepository;
import jabaclass.ai.domain.repository.RecommendationCacheRepository;
import jabaclass.ai.presentation.dto.response.RecommendationItemDto;
import jabaclass.ai.presentation.dto.response.RecommendationResponseDto;
import lombok.RequiredArgsConstructor;

/*1. 캐시 조회
2. 사용자 벡터 조회
3. 후보 클래스 조회
4. GPT 호출
5. 캐싱
6. 응답 반환*/
@Service
@RequiredArgsConstructor
public class RecommendationService implements RecommendationUseCase {
	private static final int RECOMMENDATION_COUNT = 5;

	private final RecommendationCacheRepository recommendationCacheRepository;
	private final UserVectorService userVectorService;
	private final CandidateSearchRepository candidateSearchRepository;
	private final AiGatewayPort aiGatewayPort;

	public RecommendationResponseDto recommend(UUID userId) {
		// 1. 캐시 조회
		RecommendationResponseDto cached = recommendationCacheRepository.get(userId);
		if (cached != null) {
			return cached;
		}

		// 2. 사용자 벡터 조회 (없으면 생성)
		UserVector userVector = userVectorService.getOrCreate(userId);

		// 3. 후보 조회
		List<CandidateClassDto> candidates = candidateSearchRepository.findTopK(userVector, RECOMMENDATION_COUNT);
		if (candidates == null || candidates.isEmpty()) {
			return createFallback(userId);
		}

		// 4. GPT 호출 (추천 이유 생성)
		Map<UUID, String> reasonMap = aiGatewayPort.generateRecommendationReasons(userVector, candidates);

		// 응답 생성
		List<RecommendationItemDto> items = candidates.stream()
			.map(c -> new RecommendationItemDto(
				c.productId(),
				c.title(),
				reasonMap.getOrDefault(
					c.productId(),
					"사용자 취향과 유사한 클래스입니다."
				)
			))
			.toList();
		RecommendationResponseDto response = new RecommendationResponseDto(items);

		// 6. 캐시 저장
		recommendationCacheRepository.save(userId, response);

		// 7. 반환
		return response;
	}

	private RecommendationResponseDto createFallback(UUID userId) {
		List<CandidateClassDto> popular = candidateSearchRepository.findPopular(RECOMMENDATION_COUNT);

		List<RecommendationItemDto> items = popular.stream()
			.map(p -> new RecommendationItemDto(
				p.productId(),
				p.title(),
				"현재 인기 있는 클래스입니다."
			))
			.toList();

		return new RecommendationResponseDto(items);
	}
}
