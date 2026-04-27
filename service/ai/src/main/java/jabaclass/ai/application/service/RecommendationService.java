package jabaclass.ai.application.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.core.task.TaskRejectedException;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import jabaclass.ai.application.dto.CandidateClassDto;
import jabaclass.ai.application.usecase.RecommendationUseCase;
import jabaclass.ai.domain.model.UserVector;
import jabaclass.ai.domain.repository.CandidateSearchRepository;
import jabaclass.ai.domain.repository.RecommendationCacheRepository;
import jabaclass.ai.presentation.dto.response.RecommendationItemDto;
import jabaclass.ai.presentation.dto.response.RecommendationResponseDto;
import jabaclass.ai.presentation.dto.response.RecommendationStatus;

/*1. 캐시 조회
2. 사용자 벡터 조회
3. 후보 클래스 조회
4. GPT 호출
5. 캐싱
6. 응답 반환*/
@Service
public class RecommendationService implements RecommendationUseCase {
	private static final int RECOMMENDATION_COUNT = 5;
	private static final String DEFAULT_REASON = "사용자 취향과 유사한 클래스입니다.";

	private final RecommendationCacheRepository recommendationCacheRepository;
	private final UserVectorService userVectorService;
	private final CandidateSearchRepository candidateSearchRepository;
	private final RecommendationReasonAsyncService recommendationReasonAsyncService;
	private final Counter recommendationCacheHitCounter;
	private final Counter recommendationCacheMissCounter;

	public RecommendationService(
		RecommendationCacheRepository recommendationCacheRepository,
		UserVectorService userVectorService,
		CandidateSearchRepository candidateSearchRepository,
		RecommendationReasonAsyncService recommendationReasonAsyncService,
		MeterRegistry meterRegistry
	) {
		this.recommendationCacheRepository = recommendationCacheRepository;
		this.userVectorService = userVectorService;
		this.candidateSearchRepository = candidateSearchRepository;
		this.recommendationReasonAsyncService = recommendationReasonAsyncService;
		this.recommendationCacheHitCounter = meterRegistry.counter("ai.recommendation.cache.hit");
		this.recommendationCacheMissCounter = meterRegistry.counter("ai.recommendation.cache.miss");
	}

	public RecommendationResponseDto recommend(UUID userId) {
		// 1. 캐시 조회
		RecommendationResponseDto cached = recommendationCacheRepository.get(userId);
		if (cached != null) {
			recommendationCacheHitCounter.increment();
			return cached;
		}
		recommendationCacheMissCounter.increment();

		// 2. 사용자 벡터 조회 (없으면 생성)
		UserVector userVector = userVectorService.getOrCreate(userId);

		// 3. 후보 조회
		List<CandidateClassDto> candidates = candidateSearchRepository.findTopK(userVector, RECOMMENDATION_COUNT);
		if (candidates == null || candidates.isEmpty()) {
			return createFallback(userId);
		}

		List<RecommendationItemDto> items = candidates.stream()
			.map(c -> new RecommendationItemDto(
				c.productId(),
				c.title(),
				DEFAULT_REASON
			))
			.toList();
		RecommendationResponseDto response = new RecommendationResponseDto(
			RecommendationStatus.PENDING,
			items
		);

		// 4. 기본 추천 결과를 먼저 캐시에 저장
		recommendationCacheRepository.save(userId, response);

		// 5. 추천 이유 생성은 비동기로 수행
		try {
			recommendationReasonAsyncService.generateAndCache(userId, userVector, candidates);
		} catch (TaskRejectedException e) {
			RecommendationResponseDto failedResponse = new RecommendationResponseDto(
				RecommendationStatus.FAILED,
				items
			);
			recommendationCacheRepository.save(userId, failedResponse);
			return failedResponse;
		}

		// 6. 즉시 반환
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

		return new RecommendationResponseDto(RecommendationStatus.COMPLETED, items);
	}
}
