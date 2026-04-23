package jabaclass.ai.application.service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;

import jabaclass.ai.domain.model.ActionType;
import jabaclass.ai.domain.model.UserActivity;
import jabaclass.ai.domain.model.UserVector;
import jabaclass.ai.domain.repository.ProductEmbeddingRepository;
import jabaclass.ai.domain.repository.UserActivityRepository;
import jabaclass.ai.domain.repository.UserVectorCacheRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserVectorService {

	private static final int VECTOR_SIZE = 768;
	private static final int MAX_VIEW_CONTRIBUTIONS_PER_PRODUCT = 3;
	private static final double RECENCY_HALF_LIFE_DAYS = 14.0;

	private final UserActivityRepository userActivityRepository;
	private final UserVectorCacheRepository userVectorCacheRepository;
	private final ProductEmbeddingRepository productEmbeddingRepository;

	// Redis에 있으면 가져오고 없으면 생성
	public UserVector getOrCreate(UUID userId) {

		UserVector cached = userVectorCacheRepository.get(userId);
		if (cached != null && !cached.isEmpty()) {
			return cached;
		}

		UserVector created = generate(userId);
		userVectorCacheRepository.save(userId, created);

		return created;
	}

	// UserActivity 기반 벡터 생성
	public UserVector generate(UUID userId) {
		List<UserActivity> activities = userActivityRepository.findByUserId(userId);
		activities = activities.stream()
			.sorted(Comparator.comparing(UserActivity::getCreatedAt).reversed())
			.toList();

		float[] vector = new float[VECTOR_SIZE];
		Map<UUID, Integer> viewContributionCounts = new HashMap<>();
		LocalDateTime now = LocalDateTime.now();

		for (UserActivity activity : activities) {
			if (shouldSkipView(activity, viewContributionCounts)) {
				continue;
			}

			float weight = getWeight(activity.getActionType()) * getRecencyWeight(activity.getCreatedAt(), now);

			float[] itemVector = productEmbeddingRepository.findEmbeddingByProductId(activity.getProductId());

			// null 방어
			if (itemVector == null || itemVector.length != VECTOR_SIZE) {
				continue;
			}

			// 가중치 적용
			for (int i = 0; i < VECTOR_SIZE; i++) {
				vector[i] += itemVector[i] * weight;
			}
		}

		// 정규화 (코사인 유사도 안정화)
		return new UserVector(normalize(vector));
	}

	// 행동 가중치
	private float getWeight(ActionType actionType) {
		return switch (actionType) {
			case VIEW -> 1.0f;
			case WISHLIST -> 2.0f;
			case ORDER -> 3.0f;
		};
	}

	private boolean shouldSkipView(UserActivity activity, Map<UUID, Integer> viewContributionCounts) {
		if (activity.getActionType() != ActionType.VIEW) {
			return false;
		}

		int count = viewContributionCounts.getOrDefault(activity.getProductId(), 0);
		if (count >= MAX_VIEW_CONTRIBUTIONS_PER_PRODUCT) {
			return true;
		}

		viewContributionCounts.put(activity.getProductId(), count + 1);
		return false;
	}

	private float getRecencyWeight(LocalDateTime createdAt, LocalDateTime now) {
		long ageHours = Math.max(0L, ChronoUnit.HOURS.between(createdAt, now));
		double halfLifeHours = RECENCY_HALF_LIFE_DAYS * 24.0;
		return (float) Math.pow(0.5, ageHours / halfLifeHours);
	}

	// 벡터 정규화
	private float[] normalize(float[] vector) {

		double norm = 0.0;

		for (float v : vector) {
			norm += v * v;
		}

		norm = Math.sqrt(norm);

		if (norm == 0) return vector;

		for (int i = 0; i < vector.length; i++) {
			vector[i] /= norm;
		}

		return vector;
	}
}
