package jabaclass.apigateway.application.service;

import java.util.Arrays;
import java.util.List;

import jabaclass.apigateway.config.GatewayRulesProperties;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.util.AntPathMatcher;

import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class RbacService {

	private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();
	private final GatewayRulesProperties properties;

	public Mono<Boolean> isAllowed(String path, HttpMethod method, String role) {
		List<GatewayRulesProperties.RbacEntry> matched = properties.rbac().stream()
			.filter(e -> e.method().equalsIgnoreCase(method.name())
				&& PATH_MATCHER.match(e.path(), path))
			.sorted((a, b) -> b.path().length() - a.path().length())
			.toList();

		if (matched.isEmpty()) {
			return Mono.just(true);
		}

		GatewayRulesProperties.RbacEntry best = matched.get(0);
		boolean allowed = Arrays.stream(best.allowedRoles().split(","))
			.map(String::trim)
			.anyMatch(r -> role != null && r.equalsIgnoreCase(role));

		return Mono.just(allowed);
	}
}
