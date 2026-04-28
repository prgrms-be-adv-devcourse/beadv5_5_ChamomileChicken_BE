package jabaclass.ai.application.service;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jabaclass.ai.application.dto.CandidateClassDto;
import jabaclass.ai.application.port.external.AiGatewayPort;
import jabaclass.ai.domain.model.UserVector;
import jabaclass.ai.domain.repository.RecommendationCacheRepository;
import jabaclass.ai.presentation.dto.response.RecommendationItemDto;
import jabaclass.ai.presentation.dto.response.RecommendationResponseDto;
import jabaclass.ai.presentation.dto.response.RecommendationStatus;
import lombok.extern.slf4j.Slf4j;

/**
 * 추천 이유를 비동기로 만드는 서비스
 */
@Service
@Slf4j
public class RecommendationReasonAsyncService {
	private static final String DEFAULT_REASON = "사용자 취향과 유사한 클래스입니다.";

	private final AiGatewayPort aiGatewayPort;
	private final RecommendationCacheRepository recommendationCacheRepository;
	private final Counter recommendationReasonSuccessCounter;
	private final Counter recommendationReasonFailureCounter;
	private final Timer recommendationReasonLatencyTimer;

	public RecommendationReasonAsyncService(
		AiGatewayPort aiGatewayPort,
		RecommendationCacheRepository recommendationCacheRepository,
		MeterRegistry meterRegistry
	) {
		this.aiGatewayPort = aiGatewayPort;
		this.recommendationCacheRepository = recommendationCacheRepository;
		this.recommendationReasonSuccessCounter = meterRegistry.counter("ai.recommendation.reason.success");
		this.recommendationReasonFailureCounter = meterRegistry.counter("ai.recommendation.reason.failure");
		this.recommendationReasonLatencyTimer = meterRegistry.timer("ai.recommendation.reason.latency");
	}

	@Async("recommendationReasonExecutor")
	public void generateAndCache(
		UUID userId,
		UserVector userVector,
		List<CandidateClassDto> candidates
	) {
		Timer.Sample sample = Timer.start();
		try {
			Map<UUID, String> reasonMap = aiGatewayPort.generateRecommendationReasons(userVector, candidates);
			List<RecommendationItemDto> items = candidates.stream()
				.map(candidate -> new RecommendationItemDto(
					candidate.productId(),
					candidate.title(),
					reasonMap.getOrDefault(candidate.productId(), DEFAULT_REASON)
				))
				.toList();
			recommendationCacheRepository.save(
				userId,
				new RecommendationResponseDto(RecommendationStatus.COMPLETED, items)
			);
			recommendationReasonSuccessCounter.increment();
		} catch (Exception e) {
			log.warn("Async OpenAI recommendation reason generation failed. userId={}", userId, e);
			List<RecommendationItemDto> fallbackItems = candidates.stream()
				.map(candidate -> new RecommendationItemDto(
					candidate.productId(),
					candidate.title(),
					DEFAULT_REASON
				))
				.toList();
			recommendationCacheRepository.save(
				userId,
				new RecommendationResponseDto(RecommendationStatus.FAILED, fallbackItems)
			);
			recommendationReasonFailureCounter.increment();
		} finally {
			sample.stop(recommendationReasonLatencyTimer);
		}
	}
}
