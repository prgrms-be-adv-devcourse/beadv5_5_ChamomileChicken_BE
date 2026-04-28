package jabaclass.ai.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import jabaclass.ai.application.dto.CandidateClassDto;
import jabaclass.ai.domain.model.UserPreferenceState;
import jabaclass.ai.domain.model.UserVector;
import jabaclass.ai.domain.repository.CandidateSearchRepository;
import jabaclass.ai.domain.repository.RecommendationCacheRepository;
import jabaclass.ai.presentation.dto.response.RecommendationResponseDto;
import jabaclass.ai.presentation.dto.response.RecommendationStatus;

@ExtendWith(MockitoExtension.class)
class RecommendationServiceTest {

	private RecommendationService recommendationService;

	@Mock
	private UserVectorService userVectorService;

	@Mock
	private CandidateSearchRepository candidateSearchRepository;

	@Mock
	private RecommendationReasonAsyncService recommendationReasonAsyncService;

	@Mock
	private RecommendationCacheRepository recommendationCacheRepository;

	private static final UUID USER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
	private static final UUID PRODUCT_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
	private static final UUID EXCLUDED_PRODUCT_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");

	@BeforeEach
	void setUp() {
		recommendationService = new RecommendationService(
			userVectorService,
			candidateSearchRepository,
			recommendationReasonAsyncService,
			recommendationCacheRepository
		);
	}

	@Test
	void snapshot_캐시가_없으면_pending으로_응답하고_비동기_생성을_시작한다() {
		UserVector userVector = new UserVector(new float[] { 0.1f, 0.2f });
		UserPreferenceState state = new UserPreferenceState(userVector, Set.of(EXCLUDED_PRODUCT_ID));
		CandidateClassDto candidate = candidate("도예 클래스");

		given(recommendationCacheRepository.get(USER_ID)).willReturn(null);
		given(userVectorService.getOrCreateState(USER_ID)).willReturn(state);
		given(candidateSearchRepository.findTopK(userVector, Set.of(EXCLUDED_PRODUCT_ID), 5))
			.willReturn(List.of(candidate));

		RecommendationResponseDto result = recommendationService.recommend(USER_ID);

		assertThat(result.status()).isEqualTo(RecommendationStatus.PENDING);
		assertThat(result.recommendations()).hasSize(1);
		assertThat(result.recommendations().get(0).reason()).isEqualTo("사용자 취향과 유사한 클래스입니다.");
		then(recommendationCacheRepository).should().save(
			org.mockito.ArgumentMatchers.eq(USER_ID),
			org.mockito.ArgumentMatchers.any(RecommendationResponseDto.class)
		);
		then(recommendationReasonAsyncService).should().generateAndCache(USER_ID, userVector, List.of(candidate));
	}

	@Test
	void snapshot_캐시가_있으면_그대로_반환한다() {
		RecommendationResponseDto cached = new RecommendationResponseDto(
			RecommendationStatus.COMPLETED,
			List.of(new jabaclass.ai.presentation.dto.response.RecommendationItemDto(
				PRODUCT_ID,
				"향수 클래스",
				"차분한 취향에 잘 맞는 클래스입니다."
			))
		);
		given(recommendationCacheRepository.get(USER_ID)).willReturn(cached);

		RecommendationResponseDto result = recommendationService.recommend(USER_ID);

		assertThat(result).isEqualTo(cached);
		then(userVectorService).shouldHaveNoInteractions();
		then(recommendationReasonAsyncService).shouldHaveNoInteractions();
	}

	@Test
	void 후보가_없으면_인기상품_fallback을_반환한다() {
		UserVector userVector = new UserVector(new float[] { 0.5f, 0.6f });
		UserPreferenceState state = new UserPreferenceState(userVector, Set.of(EXCLUDED_PRODUCT_ID));
		CandidateClassDto popular = candidate("베이킹 클래스");

		given(recommendationCacheRepository.get(USER_ID)).willReturn(null);
		given(userVectorService.getOrCreateState(USER_ID)).willReturn(state);
		given(candidateSearchRepository.findTopK(userVector, Set.of(EXCLUDED_PRODUCT_ID), 5)).willReturn(List.of());
		given(candidateSearchRepository.findPopular(Set.of(EXCLUDED_PRODUCT_ID), 5)).willReturn(List.of(popular));

		RecommendationResponseDto result = recommendationService.recommend(USER_ID);

		assertThat(result.status()).isEqualTo(RecommendationStatus.COMPLETED);
		assertThat(result.recommendations()).hasSize(1);
		assertThat(result.recommendations().get(0).title()).isEqualTo("베이킹 클래스");
		assertThat(result.recommendations().get(0).reason()).isEqualTo("현재 인기 있는 클래스입니다.");
		then(recommendationReasonAsyncService).shouldHaveNoInteractions();
	}

	private CandidateClassDto candidate(String title) {
		return new CandidateClassDto(
			PRODUCT_ID,
			title,
			"설명",
			new BigDecimal("50000"),
			"서울"
		);
	}
}
