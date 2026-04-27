package jabaclass.apigateway.application.service;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.util.AntPathMatcher;

import reactor.core.publisher.Mono;

import jabaclass.apigateway.config.GatewayRulesProperties;

@Service
@RequiredArgsConstructor
public class RbacService {

	private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();
	private final GatewayRulesProperties properties;

	public Mono<Boolean> isAllowed(String path, HttpMethod method, String role) {
		Optional<GatewayRulesProperties.RbacEntry> best = properties.rbac().stream()
			.filter(e -> e.method().equalsIgnoreCase(method.name())
				&& PATH_MATCHER.match(e.path(), path))
			.max(Comparator.comparingInt(e -> e.path().length()));

		if (best.isEmpty()) {
			return Mono.just(true);
		}

		List<String> allowedRoles = best.get().allowedRoles() == null ? List.of() : best.get().allowedRoles();
		boolean allowed = allowedRoles.stream()
			.anyMatch(r -> role != null && r.equalsIgnoreCase(role));

		return Mono.just(allowed);
	}
}
