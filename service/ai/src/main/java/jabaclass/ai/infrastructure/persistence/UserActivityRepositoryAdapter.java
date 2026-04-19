package jabaclass.ai.infrastructure.persistence;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import jabaclass.ai.domain.model.UserActivity;
import jabaclass.ai.domain.repository.UserActivityRepository;
import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class UserActivityRepositoryAdapter implements UserActivityRepository {

	private final UserActivityJpaRepository userActivityJpaRepository;

	@Override
	public List<UserActivity> findByUserId(UUID userId) {
		return userActivityJpaRepository.findByUserId(userId);
	}

	@Override
	public UserActivity save(UserActivity userActivity) {
		return userActivityJpaRepository.save(userActivity);
	}
}
