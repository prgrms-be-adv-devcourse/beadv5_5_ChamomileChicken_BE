package jabaclass.ai.application.service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Service;

import jabaclass.ai.domain.model.ActionType;
import jabaclass.ai.domain.model.UserActivity;
import jabaclass.ai.domain.model.UserPreferenceState;
import jabaclass.ai.domain.model.UserVector;
import jabaclass.ai.domain.model.UserVectorProfile;
import jabaclass.ai.domain.repository.ProductEmbeddingRepository;
import jabaclass.ai.domain.repository.UserActivityRepository;
import jabaclass.ai.domain.repository.UserVectorCacheRepository;
import lombok.RequiredArgsConstructor;

/**
 * 사용자 벡터 계산,관리하는 서비스
 */
@Service
@RequiredArgsConstructor
public class UserVectorService {

	private static final int CACHE_VERSION = 2;
	private static final int VECTOR_SIZE = 768;
	private static final double RECENCY_HALF_LIFE_DAYS = 14.0;

	private final UserActivityRepository userActivityRepository;
	private final UserVectorCacheRepository userVectorCacheRepository;
	private final ProductEmbeddingRepository productEmbeddingRepository;

	public UserPreferenceState getOrCreateState(UUID userId) {
		UserVectorProfile cachedProfile = userVectorCacheRepository.getProfile(userId);
		Set<UUID> excludedProductIds = userVectorCacheRepository.getExcludedProductIds(userId);

		if (isCacheUsable(cachedProfile, excludedProductIds)) {
			return new UserPreferenceState(cachedProfile.userVector(), excludedProductIds);
		}

		return rebuildState(userId);
	}

	public void updateOnActivity(UUID userId, UUID productId, ActionType actionType) {
		if (shouldExclude(actionType)) {
			userVectorCacheRepository.addExcludedProductId(userId, productId);
		}

		UserVectorProfile cachedProfile = userVectorCacheRepository.getProfile(userId);
		if (cachedProfile == null || cachedProfile.version() != CACHE_VERSION) {
			rebuildState(userId);
			return;
		}

		float[] embedding = productEmbeddingRepository.findAllByProductIds(Set.of(productId)).get(productId);
		if (embedding == null || embedding.length != VECTOR_SIZE) {
			return;
		}

		LocalDateTime now = LocalDateTime.now();
		float[] updated = applyDecay(cachedProfile.userVector().vector(), cachedProfile.lastUpdatedAt(), now);
		float weight = getWeight(actionType);
		for (int i = 0; i < VECTOR_SIZE; i++) {
			updated[i] += embedding[i] * weight;
		}

		userVectorCacheRepository.saveProfile(
			userId,
			new UserVectorProfile(new UserVector(normalize(updated)), now, CACHE_VERSION)
		);
	}

	public UserVector generate(UUID userId) {
		return generateFromActivities(sortActivities(userActivityRepository.findByUserId(userId)));
	}

	private UserPreferenceState rebuildState(UUID userId) {
		List<UserActivity> activities = sortActivities(userActivityRepository.findByUserId(userId));
		UserVector userVector = generateFromActivities(activities);
		Set<UUID> excludedProductIds = activities.stream()
			.filter(activity -> shouldExclude(activity.getActionType()))
			.map(UserActivity::getProductId)
			.collect(java.util.stream.Collectors.toUnmodifiableSet());

		LocalDateTime lastUpdatedAt = activities.isEmpty()
			? LocalDateTime.now()
			: activities.get(0).getCreatedAt();

		userVectorCacheRepository.saveProfile(
			userId,
			new UserVectorProfile(userVector, lastUpdatedAt, CACHE_VERSION)
		);
		userVectorCacheRepository.saveExcludedProductIds(userId, excludedProductIds);

		return new UserPreferenceState(userVector, excludedProductIds);
	}

	private List<UserActivity> sortActivities(List<UserActivity> activities) {
		return activities.stream()
			.sorted(Comparator.comparing(UserActivity::getCreatedAt).reversed())
			.toList();
	}

	private UserVector generateFromActivities(List<UserActivity> activities) {
		float[] vector = new float[VECTOR_SIZE];
		LocalDateTime now = LocalDateTime.now();

		Set<UUID> productIds = activities.stream()
			.map(UserActivity::getProductId)
			.collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
		Map<UUID, float[]> embeddingsByProductId = productEmbeddingRepository.findAllByProductIds(productIds);

		for (UserActivity activity : activities) {
			float weight = getWeight(activity.getActionType()) * getRecencyWeight(activity.getCreatedAt(), now);
			float[] itemVector = embeddingsByProductId.get(activity.getProductId());

			if (itemVector == null || itemVector.length != VECTOR_SIZE) {
				continue;
			}

			for (int i = 0; i < VECTOR_SIZE; i++) {
				vector[i] += itemVector[i] * weight;
			}
		}

		return new UserVector(normalize(vector));
	}

	private boolean isCacheUsable(UserVectorProfile cachedProfile, Set<UUID> excludedProductIds) {
		return cachedProfile != null
			&& cachedProfile.version() == CACHE_VERSION
			&& excludedProductIds != null;
	}

	private float getWeight(ActionType actionType) {
		return switch (actionType) {
			case VIEW -> 1.0f;
			case WISHLIST -> 3.0f;
			case ORDER -> 5.0f;
		};
	}

	private boolean shouldExclude(ActionType actionType) {
		return actionType == ActionType.WISHLIST || actionType == ActionType.ORDER;
	}

	private float getRecencyWeight(LocalDateTime createdAt, LocalDateTime now) {
		long ageHours = Math.max(0L, ChronoUnit.HOURS.between(createdAt, now));
		double halfLifeHours = RECENCY_HALF_LIFE_DAYS * 24.0;
		return (float) Math.pow(0.5, ageHours / halfLifeHours);
	}

	private float[] applyDecay(float[] vector, LocalDateTime lastUpdatedAt, LocalDateTime now) {
		float[] decayed = vector.clone();
		if (lastUpdatedAt == null) {
			return decayed;
		}

		float decayFactor = getRecencyWeight(lastUpdatedAt, now);
		for (int i = 0; i < decayed.length; i++) {
			decayed[i] *= decayFactor;
		}
		return decayed;
	}

	private float[] normalize(float[] vector) {
		double norm = 0.0;
		for (float v : vector) {
			norm += v * v;
		}

		norm = Math.sqrt(norm);
		if (norm == 0) {
			return vector;
		}

		for (int i = 0; i < vector.length; i++) {
			vector[i] /= norm;
		}
		return vector;
	}
}
