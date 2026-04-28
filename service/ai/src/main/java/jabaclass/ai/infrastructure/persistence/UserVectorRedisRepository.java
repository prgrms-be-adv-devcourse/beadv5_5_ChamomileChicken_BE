package jabaclass.ai.infrastructure.persistence;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import jabaclass.ai.domain.model.UserVector;
import jabaclass.ai.domain.model.UserVectorProfile;
import jabaclass.ai.domain.repository.UserVectorCacheRepository;
import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class UserVectorRedisRepository implements UserVectorCacheRepository {

	private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
	private static final String PROFILE_KEY_FORMAT = "user:profile:v2:%s";
	private static final String EXCLUDE_KEY_FORMAT = "user:exclude:v2:%s";
	private static final Duration TTL = Duration.ofHours(6);
	private static final int DELETE_BATCH_SIZE = 100;

	private final StringRedisTemplate redisTemplate;
	private final ObjectMapper objectMapper;

	@Override
	public UserVector get(UUID userId) {
		UserVectorProfile profile = getProfile(userId);
		return profile == null ? null : profile.userVector();
	}

	@Override
	public UserVectorProfile getProfile(UUID userId) {
		String value = redisTemplate.opsForValue().get(profileKey(userId));
		if (value == null) {
			return null;
		}

		try {
			CachedUserProfile cached = objectMapper.readValue(value, CachedUserProfile.class);
			return new UserVectorProfile(
				new UserVector(cached.vector()),
				LocalDateTime.parse(cached.lastUpdatedAt(), DATE_TIME_FORMATTER),
				cached.version()
			);
		} catch (JsonProcessingException e) {
			throw new IllegalStateException("user profile 역직렬화 실패", e);
		}
	}

	@Override
	public void save(UUID userId, UserVector userVector) {
		saveProfile(userId, new UserVectorProfile(userVector, LocalDateTime.now(), 1));
	}

	@Override
	public void saveProfile(UUID userId, UserVectorProfile profile) {
		try {
			String value = objectMapper.writeValueAsString(
				new CachedUserProfile(
					profile.userVector().vector(),
					profile.lastUpdatedAt().format(DATE_TIME_FORMATTER),
					profile.version()
				)
			);
			redisTemplate.opsForValue().set(profileKey(userId), value, TTL);
		} catch (JsonProcessingException e) {
			throw new IllegalStateException("user profile 직렬화 실패", e);
		}
	}

	@Override
	public Set<UUID> getExcludedProductIds(UUID userId) {
		String value = redisTemplate.opsForValue().get(excludeKey(userId));
		if (value == null) {
			return null;
		}

		try {
			List<UUID> excluded = objectMapper.readValue(value, new TypeReference<List<UUID>>() {
			});
			return new LinkedHashSet<>(excluded);
		} catch (JsonProcessingException e) {
			throw new IllegalStateException("excluded product ids 역직렬화 실패", e);
		}
	}

	@Override
	public void saveExcludedProductIds(UUID userId, Set<UUID> excludedProductIds) {
		try {
			String value = objectMapper.writeValueAsString(excludedProductIds == null ? Set.of() : excludedProductIds);
			redisTemplate.opsForValue().set(excludeKey(userId), value, TTL);
		} catch (JsonProcessingException e) {
			throw new IllegalStateException("excluded product ids 직렬화 실패", e);
		}
	}

	@Override
	public void addExcludedProductId(UUID userId, UUID productId) {
		Set<UUID> excludedProductIds = getExcludedProductIds(userId);
		Set<UUID> updated = excludedProductIds == null ? new LinkedHashSet<>() : new LinkedHashSet<>(excludedProductIds);
		updated.add(productId);
		saveExcludedProductIds(userId, updated);
	}

	@Override
	public void delete(UUID userId) {
		redisTemplate.delete(profileKey(userId));
	}

	@Override
	public void deleteAllProfiles() {
		ScanOptions options = ScanOptions.scanOptions()
			.match(PROFILE_KEY_FORMAT.formatted("*"))
			.count(DELETE_BATCH_SIZE)
			.build();

		try (Cursor<String> cursor = redisTemplate.scan(options)) {
			List<String> batch = new ArrayList<>(DELETE_BATCH_SIZE);
			while (cursor.hasNext()) {
				batch.add(cursor.next());
				if (batch.size() >= DELETE_BATCH_SIZE) {
					redisTemplate.delete(batch);
					batch.clear();
				}
			}

			if (!batch.isEmpty()) {
				redisTemplate.delete(batch);
			}
		}
	}

	private String profileKey(UUID userId) {
		return PROFILE_KEY_FORMAT.formatted(userId);
	}

	private String excludeKey(UUID userId) {
		return EXCLUDE_KEY_FORMAT.formatted(userId);
	}

	private record CachedUserProfile(
		float[] vector,
		String lastUpdatedAt,
		int version
	) {
	}
}
