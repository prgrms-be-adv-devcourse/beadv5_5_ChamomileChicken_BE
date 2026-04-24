package jabaclass.ai.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import jabaclass.ai.domain.model.ActionType;
import jabaclass.ai.domain.model.UserActivity;
import jabaclass.ai.domain.model.UserVector;
import jabaclass.ai.domain.repository.ProductEmbeddingRepository;
import jabaclass.ai.domain.repository.UserActivityRepository;
import jabaclass.ai.domain.repository.UserVectorCacheRepository;

@ExtendWith(MockitoExtension.class)
class UserVectorServiceTest {

	@InjectMocks
	private UserVectorService userVectorService;

	@Mock
	private UserActivityRepository userActivityRepository;

	@Mock
	private UserVectorCacheRepository userVectorCacheRepository;

	@Mock
	private ProductEmbeddingRepository productEmbeddingRepository;

	private static final UUID USER_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
	private static final UUID PRODUCT_ID = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");

	@Test
	void 캐시에_벡터가_있으면_생성하지_않고_반환한다() {
		UserVector cached = new UserVector(new float[] { 1.0f, 0.0f });
		given(userVectorCacheRepository.get(USER_ID)).willReturn(cached);

		UserVector result = userVectorService.getOrCreate(USER_ID);

		assertThat(result).isEqualTo(cached);
		then(userVectorCacheRepository).should().get(USER_ID);
		then(userActivityRepository).shouldHaveNoInteractions();
		then(productEmbeddingRepository).shouldHaveNoInteractions();
	}

	@Test
	void 캐시에_벡터가_없으면_생성후_저장한다() {
		float[] embedding = new float[768];
		embedding[0] = 3.0f;
		embedding[1] = 4.0f;
		given(userVectorCacheRepository.get(USER_ID)).willReturn(null);
		given(userActivityRepository.findByUserId(USER_ID))
			.willReturn(List.of(UserActivity.create(USER_ID, PRODUCT_ID, ActionType.VIEW)));
		given(productEmbeddingRepository.findAllByProductIds(anyCollection()))
			.willReturn(Map.of(PRODUCT_ID, embedding));

		UserVector result = userVectorService.getOrCreate(USER_ID);

		assertThat(result.vector()[0]).isEqualTo(0.6f);
		assertThat(result.vector()[1]).isEqualTo(0.8f);
		then(userVectorCacheRepository).should().save(USER_ID, result);
	}

	@Test
	void 행동_가중치를_적용해_정규화된_벡터를_생성한다() {
		UUID viewedProductId = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");
		UUID orderedProductId = UUID.fromString("dddddddd-dddd-dddd-dddd-dddddddddddd");
		float[] viewedEmbedding = new float[768];
		float[] orderedEmbedding = new float[768];
		viewedEmbedding[0] = 1.0f;
		orderedEmbedding[1] = 2.0f;

		given(userActivityRepository.findByUserId(USER_ID)).willReturn(List.of(
			UserActivity.create(USER_ID, viewedProductId, ActionType.VIEW),
			UserActivity.create(USER_ID, orderedProductId, ActionType.ORDER)
		));
		given(productEmbeddingRepository.findAllByProductIds(anyCollection()))
			.willReturn(Map.of(
				viewedProductId, viewedEmbedding,
				orderedProductId, orderedEmbedding
			));

		UserVector result = userVectorService.generate(USER_ID);

		assertThat(result.vector()[0]).isCloseTo((float) (1.0 / Math.sqrt(37.0)), within(0.0001f));
		assertThat(result.vector()[1]).isCloseTo((float) (6.0 / Math.sqrt(37.0)), within(0.0001f));
	}

	@Test
	void 차원이_다르거나_null_임베딩은_무시한다() {
		UUID invalidProductId = UUID.fromString("eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee");
		given(userActivityRepository.findByUserId(USER_ID))
			.willReturn(List.of(UserActivity.create(USER_ID, invalidProductId, ActionType.WISHLIST)));
		given(productEmbeddingRepository.findAllByProductIds(anyCollection()))
			.willReturn(Map.of(invalidProductId, new float[] { 1.0f, 2.0f }));

		UserVector result = userVectorService.generate(USER_ID);

		assertThat(result.vector()[0]).isEqualTo(0.0f);
		assertThat(result.vector()[1]).isEqualTo(0.0f);
	}

	@Test
	void 같은_상품의_반복_VIEW는_최대_세번까지만_반영한다() {
		UUID heavilyViewedProductId = UUID.fromString("11111111-1111-1111-1111-111111111111");
		UUID orderedProductId = UUID.fromString("22222222-2222-2222-2222-222222222222");
		LocalDateTime now = LocalDateTime.now();

		float[] viewedEmbedding = new float[768];
		float[] orderedEmbedding = new float[768];
		viewedEmbedding[0] = 1.0f;
		orderedEmbedding[1] = 1.0f;

		given(userActivityRepository.findByUserId(USER_ID)).willReturn(List.of(
			UserActivity.create(USER_ID, heavilyViewedProductId, ActionType.VIEW, now.minusMinutes(1)),
			UserActivity.create(USER_ID, heavilyViewedProductId, ActionType.VIEW, now.minusMinutes(2)),
			UserActivity.create(USER_ID, heavilyViewedProductId, ActionType.VIEW, now.minusMinutes(3)),
			UserActivity.create(USER_ID, heavilyViewedProductId, ActionType.VIEW, now.minusMinutes(4)),
			UserActivity.create(USER_ID, heavilyViewedProductId, ActionType.VIEW, now.minusMinutes(5)),
			UserActivity.create(USER_ID, orderedProductId, ActionType.ORDER, now.minusMinutes(6))
		));
		given(productEmbeddingRepository.findAllByProductIds(anyCollection()))
			.willReturn(Map.of(
				heavilyViewedProductId, viewedEmbedding,
				orderedProductId, orderedEmbedding
			));

		UserVector result = userVectorService.generate(USER_ID);

		assertThat(result.vector()[0]).isCloseTo(result.vector()[1], within(0.02f));
	}

	@Test
	void 최근_행동일수록_더_크게_반영된다() {
		UUID recentProductId = UUID.fromString("33333333-3333-3333-3333-333333333333");
		UUID oldProductId = UUID.fromString("44444444-4444-4444-4444-444444444444");
		LocalDateTime now = LocalDateTime.now();

		float[] recentEmbedding = new float[768];
		float[] oldEmbedding = new float[768];
		recentEmbedding[0] = 1.0f;
		oldEmbedding[1] = 1.0f;

		given(userActivityRepository.findByUserId(USER_ID)).willReturn(List.of(
			UserActivity.create(USER_ID, oldProductId, ActionType.VIEW, now.minusDays(30)),
			UserActivity.create(USER_ID, recentProductId, ActionType.VIEW, now.minusHours(1))
		));
		given(productEmbeddingRepository.findAllByProductIds(anyCollection()))
			.willReturn(Map.of(
				recentProductId, recentEmbedding,
				oldProductId, oldEmbedding
			));

		UserVector result = userVectorService.generate(USER_ID);

		assertThat(result.vector()[0]).isGreaterThan(result.vector()[1]);
	}

	@Test
	void 활동이_없으면_0벡터를_반환하고_빈_벡터로_취급한다() {
		given(userActivityRepository.findByUserId(USER_ID)).willReturn(List.of());
		given(productEmbeddingRepository.findAllByProductIds(anyCollection())).willReturn(Map.of());

		UserVector result = userVectorService.generate(USER_ID);

		assertThat(result.isEmpty()).isTrue();
		assertThat(result.vector()).hasSize(768);
		assertThat(result.vector()).containsOnly(0.0f);
	}

	private static org.assertj.core.data.Offset<Float> within(float value) {
		return org.assertj.core.data.Offset.offset(value);
	}
}
