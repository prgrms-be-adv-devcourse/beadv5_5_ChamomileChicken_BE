package jabaclass.ai.domain.repository;

import java.util.UUID;

import jabaclass.ai.domain.model.UserVector;

public interface UserVectorCacheRepository {

	UserVector get(UUID userId);

	void save(UUID userId, UserVector userVector);

	void delete(UUID userId); // 캐시 무효화용
}
