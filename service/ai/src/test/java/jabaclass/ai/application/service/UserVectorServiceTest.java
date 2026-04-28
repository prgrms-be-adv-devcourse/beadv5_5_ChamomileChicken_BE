package jabaclass.ai.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import jabaclass.ai.domain.model.ActionType;
import jabaclass.ai.domain.model.UserActivity;
import jabaclass.ai.domain.model.UserPreferenceState;
import jabaclass.ai.domain.model.UserVector;
import jabaclass.ai.domain.model.UserVectorProfile;
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
	void 프로필과_제외목록_캐시가_있으면_DB조회없이_반환한다() {
		UserVector cached = new UserVector(new float[] { 1.0f, 0.0f });
		UserVectorProfile profile = new UserVectorProfile(cached, LocalDateTime.now(), 2);
		given(userVectorCacheRepository.getProfile(USER_ID)).willReturn(profile);
		given(userVectorCacheRepository.getExcludedProductIds(USER_ID)).willReturn(Set.of(PRODUCT_ID));

		UserPreferenceState result = userVectorService.getOrCreateState(USER_ID);

		assertThat(result.userVector().vector()).containsExactly(cached.vector());
		assertThat(result.excludedProductIds()).containsExactly(PRODUCT_ID);
		then(userActivityRepository).shouldHaveNoInteractions();
		then(productEmbeddingRepository).shouldHaveNoInteractions();
	}

	@Test
	void 캐시가_없으면_DB로그로_full_rebuild하고_저장한다() {
		float[] embedding = new float[768];
		embedding[0] = 3.0f;
		embedding[1] = 4.0f;
		given(userVectorCacheRepository.getProfile(USER_ID)).willReturn(null);
		given(userVectorCacheRepository.getExcludedProductIds(USER_ID)).willReturn(null);
		given(userActivityRepository.findByUserId(USER_ID))
			.willReturn(List.of(UserActivity.create(USER_ID, PRODUCT_ID, ActionType.WISHLIST)));
		given(productEmbeddingRepository.findAllByProductIds(anyCollection()))
			.willReturn(Map.of(PRODUCT_ID, embedding));

		UserPreferenceState result = userVectorService.getOrCreateState(USER_ID);

		assertThat(result.userVector().vector()[0]).isEqualTo(0.6f);
		assertThat(result.userVector().vector()[1]).isEqualTo(0.8f);
		assertThat(result.excludedProductIds()).containsExactly(PRODUCT_ID);
		then(userVectorCacheRepository).should().saveProfile(
			org.mockito.ArgumentMatchers.eq(USER_ID),
			org.mockito.ArgumentMatchers.any(UserVectorProfile.class)
		);
		then(userVectorCacheRepository).should().saveExcludedProductIds(USER_ID, Set.of(PRODUCT_ID));
	}

	@Test
	void 조회와_주문_모두_제외목록에_추가한다() {
		UserVector cached = new UserVector(new float[768]);
		UserVectorProfile profile = new UserVectorProfile(cached, LocalDateTime.now().minusHours(1), 2);
		float[] embedding = new float[768];
		embedding[0] = 1.0f;
		given(userVectorCacheRepository.getProfile(USER_ID)).willReturn(profile);
		given(productEmbeddingRepository.findAllByProductIds(anyCollection()))
			.willReturn(Map.of(PRODUCT_ID, embedding));

		userVectorService.updateOnActivity(USER_ID, PRODUCT_ID, ActionType.VIEW);
		userVectorService.updateOnActivity(USER_ID, PRODUCT_ID, ActionType.ORDER);

		then(userVectorCacheRepository).should(org.mockito.Mockito.times(2))
			.addExcludedProductId(USER_ID, PRODUCT_ID);
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

		assertThat(result.vector()[0]).isCloseTo((float) (1.0 / Math.sqrt(101.0)), within(0.0001f));
		assertThat(result.vector()[1]).isCloseTo((float) (10.0 / Math.sqrt(101.0)), within(0.0001f));
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

	private static org.assertj.core.data.Offset<Float> within(float value) {
		return org.assertj.core.data.Offset.offset(value);
	}
}
