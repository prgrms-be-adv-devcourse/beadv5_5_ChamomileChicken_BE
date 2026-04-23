package jabaclass.ai.application.service;

import static org.mockito.BDDMockito.then;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import jabaclass.ai.domain.model.ActionType;
import jabaclass.ai.domain.repository.RecommendationCacheRepository;
import jabaclass.ai.domain.repository.UserActivityRepository;
import jabaclass.ai.domain.repository.UserVectorCacheRepository;

@ExtendWith(MockitoExtension.class)
class UserActivityServiceTest {

	@Mock
	private UserActivityRepository userActivityRepository;

	@Mock
	private UserVectorCacheRepository userVectorCacheRepository;

	@Mock
	private RecommendationCacheRepository recommendationCacheRepository;

	@InjectMocks
	private UserActivityService userActivityService;

	@Test
	void 공통_행동_저장_메서드로_조회_외_행동도_저장할_수_있다() {
		UUID userId = UUID.randomUUID();
		UUID productId = UUID.randomUUID();

		userActivityService.recordActivity(userId, productId, ActionType.ORDER);

		then(userActivityRepository).should()
			.save(org.mockito.ArgumentMatchers.argThat(activity ->
				activity.getUserId().equals(userId)
					&& activity.getProductId().equals(productId)
					&& activity.getActionType() == ActionType.ORDER
			));
		then(userVectorCacheRepository).should().delete(userId);
		then(recommendationCacheRepository).should().delete(userId);
	}
}
