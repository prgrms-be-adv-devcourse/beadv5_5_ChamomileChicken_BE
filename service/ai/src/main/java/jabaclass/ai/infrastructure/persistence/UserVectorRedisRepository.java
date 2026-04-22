package jabaclass.ai.infrastructure.persistence;

import java.time.Duration;
import java.util.UUID;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import jabaclass.ai.domain.model.UserVector;
import jabaclass.ai.domain.repository.UserVectorCacheRepository;
import lombok.RequiredArgsConstructor;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@Repository
@RequiredArgsConstructor
public class UserVectorRedisRepository implements UserVectorCacheRepository {

	private static final String KEY_FORMAT = "user:%s:vector";
	private static final Duration TTL = Duration.ofHours(1);

	private final StringRedisTemplate redisTemplate;
	private final ObjectMapper objectMapper;

	@Override
	public UserVector get(UUID userId) {
		String key = KEY_FORMAT.formatted(userId);

		String value = redisTemplate.opsForValue().get(key);

		if (value == null) {
			return null;
		}

		try {
			float[] vector = objectMapper.readValue(value, float[].class);
			return new UserVector(vector);
		} catch (JsonProcessingException e) {
			throw new IllegalStateException("user vector 역직렬화 실패", e);
		}
	}

	@Override
	public void save(UUID userId, UserVector userVector) {
		String key = KEY_FORMAT.formatted(userId);

		try {
			String value = objectMapper.writeValueAsString(userVector.vector());
			redisTemplate.opsForValue().set(key, value, TTL);
		} catch (JsonProcessingException e) {
			throw new IllegalStateException("user vector 직렬화 실패", e);
		}
	}

	@Override
	public void delete(UUID userId) {
		String key = KEY_FORMAT.formatted(userId);
		redisTemplate.delete(key);
	}



}
