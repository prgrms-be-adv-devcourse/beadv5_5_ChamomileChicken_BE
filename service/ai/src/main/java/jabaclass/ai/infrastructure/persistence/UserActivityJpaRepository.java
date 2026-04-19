package jabaclass.ai.infrastructure.persistence;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import jabaclass.ai.domain.model.UserActivity;

public interface UserActivityJpaRepository extends JpaRepository<UserActivity, UUID> {
	List<UserActivity> findByUserId(UUID userId);
}
