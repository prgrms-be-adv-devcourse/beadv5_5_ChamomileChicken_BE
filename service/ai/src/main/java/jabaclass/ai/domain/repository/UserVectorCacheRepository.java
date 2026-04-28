package jabaclass.ai.domain.repository;

import java.util.Set;
import java.util.UUID;

import jabaclass.ai.domain.model.UserVector;
import jabaclass.ai.domain.model.UserVectorProfile;

public interface UserVectorCacheRepository {

	UserVector get(UUID userId);

	UserVectorProfile getProfile(UUID userId);

	void save(UUID userId, UserVector userVector);

	void saveProfile(UUID userId, UserVectorProfile profile);

	Set<UUID> getExcludedProductIds(UUID userId);

	void saveExcludedProductIds(UUID userId, Set<UUID> excludedProductIds);

	void addExcludedProductId(UUID userId, UUID productId);

	void delete(UUID userId); // 프로필 캐시 무효화용

	void deleteAllProfiles();
}
