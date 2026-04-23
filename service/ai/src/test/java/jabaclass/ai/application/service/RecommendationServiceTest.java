package jabaclass.ai.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import jabaclass.ai.application.dto.CandidateClassDto;
import jabaclass.ai.application.port.external.AiGatewayPort;
import jabaclass.ai.domain.model.UserVector;
import jabaclass.ai.domain.repository.CandidateSearchRepository;
import jabaclass.ai.domain.repository.RecommendationCacheRepository;
import jabaclass.ai.presentation.dto.response.RecommendationItemDto;
import jabaclass.ai.presentation.dto.response.RecommendationResponseDto;

@ExtendWith(MockitoExtension.class)
class RecommendationServiceTest {

	@InjectMocks
	private RecommendationService recommendationService;

	@Mock
	private RecommendationCacheRepository recommendationCacheRepository;

	@Mock
	private UserVectorService userVectorService;

	@Mock
	private CandidateSearchRepository candidateSearchRepository;

	@Mock
	private AiGatewayPort aiGatewayPort;

	private static final UUID USER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
	private static final UUID PRODUCT_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");

	@Test
	void 캐시에_추천이_있으면_캐시를_바로_반환한다() {
		RecommendationResponseDto cached = new RecommendationResponseDto(
			List.of(new RecommendationItemDto(PRODUCT_ID, "클래스", "캐시된 추천입니다."))
		);
		given(recommendationCacheRepository.get(USER_ID)).willReturn(cached);

		RecommendationResponseDto result = recommendationService.recommend(USER_ID);

		assertThat(result).isEqualTo(cached);
		then(recommendationCacheRepository).should().get(USER_ID);
		then(userVectorService).shouldHaveNoInteractions();
		then(candidateSearchRepository).shouldHaveNoInteractions();
		then(aiGatewayPort).shouldHaveNoInteractions();
	}

	@Test
	void 후보가_있으면_GPT_사유를_포함해_추천을_반환하고_캐시한다() {
		UserVector userVector = new UserVector(new float[] { 0.1f, 0.2f });
		CandidateClassDto candidate = new CandidateClassDto(
			PRODUCT_ID,
			"도예 클래스",
			"핸드빌딩 원데이 클래스",
			new BigDecimal("50000"),
			"서울 성수동"
		);
		given(recommendationCacheRepository.get(USER_ID)).willReturn(null);
		given(userVectorService.getOrCreate(USER_ID)).willReturn(userVector);
		given(candidateSearchRepository.findTopK(userVector, 5)).willReturn(List.of(candidate));
		given(aiGatewayPort.generateRecommendationReasons(userVector, List.of(candidate)))
			.willReturn(Map.of(PRODUCT_ID, "차분한 취향에 잘 맞는 클래스입니다."));

		RecommendationResponseDto result = recommendationService.recommend(USER_ID);

		assertThat(result.recommendations()).hasSize(1);
		assertThat(result.recommendations().get(0).productId()).isEqualTo(PRODUCT_ID);
		assertThat(result.recommendations().get(0).title()).isEqualTo("도예 클래스");
		assertThat(result.recommendations().get(0).reason()).isEqualTo("차분한 취향에 잘 맞는 클래스입니다.");
		then(recommendationCacheRepository).should().save(USER_ID, result);
	}

	@Test
	void GPT_사유가_없으면_기본_사유를_사용한다() {
		UserVector userVector = new UserVector(new float[] { 0.3f, 0.4f });
		CandidateClassDto candidate = new CandidateClassDto(
			PRODUCT_ID,
			"향수 클래스",
			"조향 입문 클래스",
			new BigDecimal("70000"),
			"서울 연남동"
		);
		given(recommendationCacheRepository.get(USER_ID)).willReturn(null);
		given(userVectorService.getOrCreate(USER_ID)).willReturn(userVector);
		given(candidateSearchRepository.findTopK(userVector, 5)).willReturn(List.of(candidate));
		given(aiGatewayPort.generateRecommendationReasons(userVector, List.of(candidate)))
			.willReturn(Map.of());

		RecommendationResponseDto result = recommendationService.recommend(USER_ID);

		assertThat(result.recommendations()).hasSize(1);
		assertThat(result.recommendations().get(0).reason()).isEqualTo("사용자 취향과 유사한 클래스입니다.");
		then(recommendationCacheRepository).should().save(USER_ID, result);
	}

	@Test
	void 후보가_없으면_인기상품_fallback_추천을_캐시한다() {
		UserVector userVector = new UserVector(new float[] { 0.5f, 0.6f });
		CandidateClassDto popular = new CandidateClassDto(
			PRODUCT_ID,
			"베이킹 클래스",
			"디저트 만들기",
			new BigDecimal("45000"),
			"서울 망원동"
		);
		given(recommendationCacheRepository.get(USER_ID)).willReturn(null);
		given(userVectorService.getOrCreate(USER_ID)).willReturn(userVector);
		given(candidateSearchRepository.findTopK(userVector, 5)).willReturn(List.of());
		given(candidateSearchRepository.findPopular(5)).willReturn(List.of(popular));

		RecommendationResponseDto result = recommendationService.recommend(USER_ID);

		assertThat(result.recommendations()).hasSize(1);
		assertThat(result.recommendations().get(0).title()).isEqualTo("베이킹 클래스");
		assertThat(result.recommendations().get(0).reason()).isEqualTo("현재 인기 있는 클래스입니다.");
		then(aiGatewayPort).shouldHaveNoInteractions();
		then(recommendationCacheRepository).should().save(USER_ID, result);
	}
}
