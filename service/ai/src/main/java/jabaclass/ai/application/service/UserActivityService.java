package jabaclass.ai.application.service;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jabaclass.ai.domain.model.ActionType;
import jabaclass.ai.domain.model.UserActivity;
import jabaclass.ai.domain.repository.RecommendationCacheRepository;
import jabaclass.ai.domain.repository.UserActivityRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 사용자의 조회/찜/주문 행동 로그를 저장하는 서비스
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserActivityService {

	private final UserActivityRepository userActivityRepository;
	private final UserVectorService userVectorService;
	private final RecommendationCacheRepository recommendationCacheRepository;

	@Transactional
	public void recordActivity(UUID userId, UUID productId, ActionType actionType) {
		userActivityRepository.save(UserActivity.create(userId, productId, actionType));
		recommendationCacheRepository.delete(userId);

		try {
			userVectorService.updateOnActivity(userId, productId, actionType);
		} catch (RuntimeException e) {
			log.warn(
				"userVector 증분 업데이트 실패. userId={}, productId={}, actionType={}",
				userId,
				productId,
				actionType,
				e
			);
		}
	}
}
