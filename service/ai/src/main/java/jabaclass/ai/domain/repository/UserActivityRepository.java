package jabaclass.ai.domain.repository;

import java.util.List;
import java.util.UUID;

import jabaclass.ai.domain.model.UserActivity;

public interface UserActivityRepository {

	List<UserActivity> findByUserId(UUID userId);

	UserActivity save(UserActivity activity);
}
