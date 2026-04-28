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

@ExtendWith(MockitoExtension.class)
class UserActivityServiceTest {

	@Mock
	private UserActivityRepository userActivityRepository;

	@Mock
	private UserVectorService userVectorService;

	@Mock
	private RecommendationCacheRepository recommendationCacheRepository;

	@InjectMocks
	private UserActivityService userActivityService;

	@Test
	void 행동_로그를_저장하고_userVector를_증분_업데이트한다() {
		UUID userId = UUID.randomUUID();
		UUID productId = UUID.randomUUID();

		userActivityService.recordActivity(userId, productId, ActionType.ORDER);

		then(userActivityRepository).should()
			.save(org.mockito.ArgumentMatchers.argThat(activity ->
				activity.getUserId().equals(userId)
					&& activity.getProductId().equals(productId)
					&& activity.getActionType() == ActionType.ORDER
			));
		then(recommendationCacheRepository).should().delete(userId);
		then(userVectorService).should().updateOnActivity(userId, productId, ActionType.ORDER);
	}
}
