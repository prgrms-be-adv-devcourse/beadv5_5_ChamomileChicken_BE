package jabaclass.ai.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import java.util.List;
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
		given(productEmbeddingRepository.findEmbeddingByProductId(PRODUCT_ID)).willReturn(embedding);

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
		given(productEmbeddingRepository.findEmbeddingByProductId(viewedProductId)).willReturn(viewedEmbedding);
		given(productEmbeddingRepository.findEmbeddingByProductId(orderedProductId)).willReturn(orderedEmbedding);

		UserVector result = userVectorService.generate(USER_ID);

		assertThat(result.vector()[0]).isCloseTo((float) (1.0 / Math.sqrt(37.0)), within(0.0001f));
		assertThat(result.vector()[1]).isCloseTo((float) (6.0 / Math.sqrt(37.0)), within(0.0001f));
	}

	@Test
	void 차원이_다르거나_null_임베딩은_무시한다() {
		UUID invalidProductId = UUID.fromString("eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee");
		given(userActivityRepository.findByUserId(USER_ID))
			.willReturn(List.of(UserActivity.create(USER_ID, invalidProductId, ActionType.CART)));
		given(productEmbeddingRepository.findEmbeddingByProductId(invalidProductId)).willReturn(new float[] { 1.0f, 2.0f });

		UserVector result = userVectorService.generate(USER_ID);

		assertThat(result.vector()[0]).isEqualTo(0.0f);
		assertThat(result.vector()[1]).isEqualTo(0.0f);
	}

	private static org.assertj.core.data.Offset<Float> within(float value) {
		return org.assertj.core.data.Offset.offset(value);
	}
}
