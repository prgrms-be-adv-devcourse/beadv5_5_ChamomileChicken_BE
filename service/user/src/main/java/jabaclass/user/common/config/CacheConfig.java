package jabaclass.user.common.config;

import java.time.Duration;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.github.benmanes.caffeine.cache.Caffeine;

@Configuration
@EnableCaching
public class CacheConfig {

	@Bean
	public CacheManager cacheManager() {
		CaffeineCacheManager manager = new CaffeineCacheManager("tokenStatus");
		manager.setCaffeine(
			Caffeine.newBuilder()
				.expireAfterWrite(Duration.ofSeconds(60))
				.maximumSize(10_000)
		);

		return manager;
	}
}
