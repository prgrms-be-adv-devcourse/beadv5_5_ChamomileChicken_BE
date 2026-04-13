package jabaclass.apigateway.config;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RouteConfig {

	@Bean
	public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
		return builder.routes()
			// File Service
			.route("file", r -> r.path("/api/v1/files/**")
				.uri("http://localhost:9000"))

			// Payment Service
			.route("payment", r -> r.path("/api/v1/payments/**")
				.uri("http://localhost:9001"))

			// Settlement Service
			.route("settlement", r -> r.path("/api/v1/settlements/**")
				.uri("http://localhost:9002"))

			// User Service
			.route("user", r -> r.path("/api/v1/auth/**", "/api/v1/users/**",
					"/api/v1/email/**", "/api/v1/deposits/**")
				.uri("http://localhost:9003"))

			// Product Service
			.route("product", r -> r.path("/api/v1/products/**")
				.uri("http://localhost:9004"))

			// Order Service
			.route("order", r -> r.path("/api/v1/orders/**")
				.uri("http://localhost:9005"))

			// Admin Service
			.route("admin", r -> r.path("/api/v1/admins/**")
				.uri("http://localhost:9007"))

			.build();
	}
}
