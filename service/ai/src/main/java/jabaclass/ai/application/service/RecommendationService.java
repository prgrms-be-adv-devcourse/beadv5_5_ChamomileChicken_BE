package jabaclass.ai.application.service;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.core.task.TaskRejectedException;
import org.springframework.stereotype.Service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jabaclass.ai.application.dto.CandidateClassDto;
import jabaclass.ai.application.usecase.RecommendationUseCase;
import jabaclass.ai.domain.model.UserPreferenceState;
import jabaclass.ai.domain.model.UserVector;
import jabaclass.ai.domain.repository.CandidateSearchRepository;
import jabaclass.ai.domain.repository.RecommendationCacheRepository;
import jabaclass.ai.presentation.dto.response.RecommendationItemDto;
import jabaclass.ai.presentation.dto.response.RecommendationResponseDto;
import jabaclass.ai.presentation.dto.response.RecommendationStatus;
import lombok.extern.slf4j.Slf4j;

/**
 * 메인 서비스
 */
@Service
@Slf4j
public class RecommendationService implements RecommendationUseCase {
	private static final int RECOMMENDATION_COUNT = 5;
	private static final String DEFAULT_REASON = "사용자 취향과 유사한 클래스입니다.";

	private final UserVectorService userVectorService;
	private final CandidateSearchRepository candidateSearchRepository;
	private final RecommendationReasonAsyncService recommendationReasonAsyncService;
	private final RecommendationCacheRepository recommendationCacheRepository;
	private final Counter recommendationRequestCounter;
	private final Counter recommendationCacheHitCounter;
	private final Counter recommendationCacheMissCounter;
	private final Counter recommendationFallbackCounter;
	private final Timer recommendationLatencyTimer;

	public RecommendationService(
		UserVectorService userVectorService,
		CandidateSearchRepository candidateSearchRepository,
		RecommendationReasonAsyncService recommendationReasonAsyncService,
		RecommendationCacheRepository recommendationCacheRepository,
		MeterRegistry meterRegistry
	) {
		this.userVectorService = userVectorService;
		this.candidateSearchRepository = candidateSearchRepository;
		this.recommendationReasonAsyncService = recommendationReasonAsyncService;
		this.recommendationCacheRepository = recommendationCacheRepository;
		this.recommendationRequestCounter = meterRegistry.counter("ai.recommendation.request.count");
		this.recommendationCacheHitCounter = meterRegistry.counter("ai.recommendation.cache.hit");
		this.recommendationCacheMissCounter = meterRegistry.counter("ai.recommendation.cache.miss");
		this.recommendationFallbackCounter = meterRegistry.counter("ai.recommendation.fallback.count");
		this.recommendationLatencyTimer = meterRegistry.timer("ai.recommendation.request.latency");
	}

	@Override
	public RecommendationResponseDto recommend(UUID userId) {
		// 추천 API 전체 호출량 & end-to-end 처리 시간 측정
		recommendationRequestCounter.increment();
		Timer.Sample sample = Timer.start();
		try {
			RecommendationResponseDto cached = recommendationCacheRepository.get(userId);
			if (cached != null) {
				recommendationCacheHitCounter.increment();
				return cached;
			}

			recommendationCacheMissCounter.increment();

			UserPreferenceState state = userVectorService.getOrCreateState(userId);
			UserVector userVector = state.userVector();
			Set<UUID> excludedProductIds = state.excludedProductIds();

			List<CandidateClassDto> candidates = candidateSearchRepository.findTopK(
				userVector,
				excludedProductIds,
				RECOMMENDATION_COUNT
			);
			if (candidates == null || candidates.isEmpty()) {
				// 개인화 추천 후보가 없으면 인기 상품 fallback으로 전환
				recommendationFallbackCounter.increment();
				return createFallback(excludedProductIds);
			}

			RecommendationResponseDto pendingResponse = buildResponse(RecommendationStatus.PENDING, candidates);
			recommendationCacheRepository.save(userId, pendingResponse);
			try {
				recommendationReasonAsyncService.generateAndCache(userId, userVector, candidates);
			} catch (TaskRejectedException e) {
				log.warn("추천 이유 비동기 작업 등록 실패. 기본 사유로 응답합니다.", e);
				RecommendationResponseDto failedResponse = buildResponse(RecommendationStatus.FAILED, candidates);
				recommendationCacheRepository.save(userId, failedResponse);
				return failedResponse;
			}

			return pendingResponse;
		} finally {
			sample.stop(recommendationLatencyTimer);
		}
	}

	private RecommendationResponseDto buildResponse(RecommendationStatus status, List<CandidateClassDto> candidates) {
		List<RecommendationItemDto> items = candidates.stream()
			.map(candidate -> new RecommendationItemDto(
				candidate.productId(),
				candidate.title(),
				DEFAULT_REASON
			))
			.toList();

		return new RecommendationResponseDto(status, items);
	}

	private RecommendationResponseDto createFallback(Set<UUID> excludedProductIds) {
		List<CandidateClassDto> popular = candidateSearchRepository.findPopular(excludedProductIds, RECOMMENDATION_COUNT);

		List<RecommendationItemDto> items = popular.stream()
			.map(popularItem -> new RecommendationItemDto(
				popularItem.productId(),
				popularItem.title(),
				"현재 인기 있는 클래스입니다."
			))
			.toList();

		return new RecommendationResponseDto(RecommendationStatus.COMPLETED, items);
	}
}
