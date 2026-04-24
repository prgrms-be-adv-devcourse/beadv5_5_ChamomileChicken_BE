package jabaclass.apigateway.config;

import java.util.List;

import lombok.RequiredArgsConstructor;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

@Configuration
@RequiredArgsConstructor
public class CorsConfig {

	private final CorsProperties corsProperties;

	@Bean
	@Order(Ordered.HIGHEST_PRECEDENCE)
	public CorsWebFilter corsWebFilter() {
		CorsConfiguration config = new CorsConfiguration();

		config.setAllowedOrigins(corsProperties.getAllowedOrigins());
		config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
		config.setAllowedHeaders(List.of("*"));
		config.setAllowCredentials(true);
		config.setMaxAge(3600L);

		// Vue.js에서 커스텀 헤더가 안 보일 때 주석 해제
		// config.setExposedHeaders(List.of("X-Total-Count", "X-Custom-Header"));

		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/**", config);

		return new CorsWebFilter(source);
	}
}
