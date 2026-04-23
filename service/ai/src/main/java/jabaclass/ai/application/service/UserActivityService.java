package jabaclass.ai.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jabaclass.ai.domain.model.ActionType;
import jabaclass.ai.domain.model.UserActivity;
import jabaclass.ai.domain.repository.RecommendationCacheRepository;
import jabaclass.ai.domain.repository.UserActivityRepository;
import jabaclass.ai.domain.repository.UserVectorCacheRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserActivityService {

	private final UserActivityRepository userActivityRepository;
	private final UserVectorCacheRepository userVectorCacheRepository;
	private final RecommendationCacheRepository recommendationCacheRepository;

	@Transactional
	public void recordActivity(java.util.UUID userId, java.util.UUID productId, ActionType actionType) {
		userActivityRepository.save(UserActivity.create(userId, productId, actionType));
		userVectorCacheRepository.delete(userId);
		recommendationCacheRepository.delete(userId);
	}
}
