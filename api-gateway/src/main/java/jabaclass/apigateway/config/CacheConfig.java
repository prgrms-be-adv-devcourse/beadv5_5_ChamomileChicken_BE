package jabaclass.apigateway.config;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import jabaclass.apigateway.domain.model.RoutePolicy;
import jabaclass.apigateway.domain.model.WhitelistEntry;

@Configuration
public class CacheConfig {

	@Bean
	public Cache<String , List<WhitelistEntry>> whitelistCache() {
		return Caffeine.newBuilder()
			.expireAfterWrite(10, TimeUnit.MINUTES)
			.maximumSize(1)
			.build();
	}

	@Bean
	public Cache<String, List<RoutePolicy>> routePolicyCache() {
		return Caffeine.newBuilder()
			.expireAfterWrite(3, TimeUnit.MINUTES)
			.maximumSize(1)
			.build();
	}
}
