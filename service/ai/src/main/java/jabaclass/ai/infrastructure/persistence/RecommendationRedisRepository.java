package jabaclass.ai.infrastructure.persistence;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import jabaclass.ai.domain.repository.RecommendationCacheRepository;
import jabaclass.ai.presentation.dto.response.RecommendationResponseDto;
import lombok.RequiredArgsConstructor;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@Repository
@RequiredArgsConstructor
public class RecommendationRedisRepository implements RecommendationCacheRepository {

	private static final String KEY_FORMAT = "user:%s:recommendation";
	private static final Duration TTL = Duration.ofMinutes(20);
	private static final int DELETE_BATCH_SIZE = 100;

	private final StringRedisTemplate redisTemplate;
	private final ObjectMapper objectMapper;

	@Override
	public RecommendationResponseDto get(UUID userId) {
		String key = KEY_FORMAT.formatted(userId);

		String value = redisTemplate.opsForValue().get(key);
		if (value == null) {
			return null;
		}

		try {
			return objectMapper.readValue(value, RecommendationResponseDto.class);
		} catch (JsonProcessingException e) {
			throw new IllegalStateException("추천 캐시 역직렬화 실패", e);
		}
	}

	@Override
	public void save(UUID userId, RecommendationResponseDto response) {
		String key = KEY_FORMAT.formatted(userId);

		try {
			String value = objectMapper.writeValueAsString(response);
			redisTemplate.opsForValue().set(key, value, TTL);
		} catch (JsonProcessingException e) {
			throw new IllegalStateException("추천 캐시 직렬화 실패", e);
		}
	}

	@Override
	public void delete(UUID userId) {
		String key = KEY_FORMAT.formatted(userId);
		redisTemplate.delete(key);
	}

	@Override
	public void deleteAll() {
		ScanOptions options = ScanOptions.scanOptions()
			.match(KEY_FORMAT.formatted("*"))
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
}
