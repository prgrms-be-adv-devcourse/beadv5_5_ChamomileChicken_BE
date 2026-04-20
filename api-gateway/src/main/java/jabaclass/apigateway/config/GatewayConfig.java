package jabaclass.apigateway.config;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import reactor.core.publisher.Mono;

@Configuration
public class GatewayConfig {

	@Bean
	public KeyResolver ipKeyResolver() {
		return exchange -> {
			String xff = exchange.getRequest().getHeaders().getFirst("X-Forwarded-For");
			String ip = (xff != null && !xff.isBlank())
				? xff.split(",")[0].trim()
				: (exchange.getRequest().getRemoteAddress() != null
				? exchange.getRequest().getRemoteAddress().getAddress().getHostAddress()
				: "unknown");
			return Mono.just(ip);
		};
	}
}
