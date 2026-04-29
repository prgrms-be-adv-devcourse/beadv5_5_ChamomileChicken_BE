package jabaclass.user.common.config;

import java.time.Duration;
import java.util.Map;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig.SlidingWindowType;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;

@Configuration
public class RedisCircuitBreakerConfig {

	@Bean
	public CircuitBreakerRegistry circuitBreakerRegistry() {
		CircuitBreakerConfig readConfig = CircuitBreakerConfig.custom()
			.slidingWindowType(SlidingWindowType.COUNT_BASED)
			.slidingWindowSize(5)
			.minimumNumberOfCalls(3)
			.failureRateThreshold(60)
			.waitDurationInOpenState(Duration.ofSeconds(20))
			.permittedNumberOfCallsInHalfOpenState(2)
			.build();

		CircuitBreakerConfig writeConfig = CircuitBreakerConfig.custom()
			.slidingWindowType(SlidingWindowType.COUNT_BASED)
			.slidingWindowSize(5)
			.minimumNumberOfCalls(3)
			.failureRateThreshold(80)
			.waitDurationInOpenState(Duration.ofSeconds(30))
			.permittedNumberOfCallsInHalfOpenState(2)
			.build();

		return CircuitBreakerRegistry.of(Map.of(
			"redis-read", readConfig,
			"redis-write", writeConfig
		));
	}
}
