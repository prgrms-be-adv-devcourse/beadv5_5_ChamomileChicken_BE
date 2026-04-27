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
import lombok.extern.slf4j.Slf4j;

/*1. 캐시 조회
2. 사용자 벡터 조회
3. 후보 클래스 조회
4. GPT 호출
5. 캐싱
6. 응답 반환*/
@Service
@RequiredArgsConstructor
@Slf4j
public class RecommendationService implements RecommendationUseCase {
	private static final int RECOMMENDATION_COUNT = 5;

	private final RecommendationCacheRepository recommendationCacheRepository;
	private final UserVectorService userVectorService;
	private final CandidateSearchRepository candidateSearchRepository;
	private final AiGatewayPort aiGatewayPort;

	public RecommendationResponseDto recommend(UUID userId) {
		long startedAt = System.currentTimeMillis();
		log.info("[RECOMMEND] start userId={}", userId);

		// 1. 캐시 조회
		log.info("[RECOMMEND] cache lookup start userId={}", userId);
		RecommendationResponseDto cached = recommendationCacheRepository.get(userId);
		if (cached != null) {
			log.info("[RECOMMEND] cache hit userId={} itemCount={} elapsedMs={}",
				userId, cached.recommendations().size(), System.currentTimeMillis() - startedAt);
			return cached;
		}
		log.info("[RECOMMEND] cache miss userId={}", userId);

		// 2. 사용자 벡터 조회 (없으면 생성)
		log.info("[RECOMMEND] user vector start userId={}", userId);
		UserVector userVector = userVectorService.getOrCreate(userId);
		log.info("[RECOMMEND] user vector ready userId={} isEmpty={}", userId, userVector.isEmpty());

		// 3. 후보 조회
		log.info("[RECOMMEND] candidate search start userId={} limit={}", userId, RECOMMENDATION_COUNT);
		List<CandidateClassDto> candidates = candidateSearchRepository.findTopK(userVector, RECOMMENDATION_COUNT);
		log.info("[RECOMMEND] candidate search result userId={} candidateCount={}",
			userId, candidates == null ? 0 : candidates.size());
		if (candidates == null || candidates.isEmpty()) {
			log.warn("[RECOMMEND] no candidates, using fallback userId={}", userId);
			return createFallback(userId);
		}

		// 4. GPT 호출 (추천 이유 생성)
		log.info("[RECOMMEND] reason generation start userId={} candidateCount={}", userId, candidates.size());
		Map<UUID, String> reasonMap = aiGatewayPort.generateRecommendationReasons(userVector, candidates);
		log.info("[RECOMMEND] reason generation complete userId={} reasonCount={}", userId, reasonMap.size());

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
		log.info("[RECOMMEND] cache save start userId={} itemCount={}", userId, items.size());
		recommendationCacheRepository.save(userId, response);
		log.info("[RECOMMEND] cache save complete userId={}", userId);

		// 7. 반환
		log.info("[RECOMMEND] success userId={} itemCount={} elapsedMs={}",
			userId, items.size(), System.currentTimeMillis() - startedAt);
		return response;
	}

	private RecommendationResponseDto createFallback(UUID userId) {
		long startedAt = System.currentTimeMillis();
		log.info("[RECOMMEND] fallback start userId={} limit={}", userId, RECOMMENDATION_COUNT);

		List<CandidateClassDto> popular = candidateSearchRepository.findPopular(RECOMMENDATION_COUNT);
		log.info("[RECOMMEND] fallback popular result userId={} candidateCount={}", userId, popular.size());

		List<RecommendationItemDto> items = popular.stream()
			.map(p -> new RecommendationItemDto(
				p.productId(),
				p.title(),
				"현재 인기 있는 클래스입니다."
			))
			.toList();

		log.info("[RECOMMEND] fallback success userId={} itemCount={} elapsedMs={}",
			userId, items.size(), System.currentTimeMillis() - startedAt);
		return new RecommendationResponseDto(items);
	}
}
