package jabaclass.apigateway.application.service;

import java.util.Arrays;
import java.util.List;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.util.AntPathMatcher;

import com.github.benmanes.caffeine.cache.Cache;
import reactor.core.publisher.Mono;

import jabaclass.apigateway.domain.model.RoutePolicy;
import jabaclass.apigateway.domain.repository.RoutePolicyRepository;

@Service
@RequiredArgsConstructor
public class RbacService {

	private static final String CACHE_KEY = "route_policy";
	private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

	private final RoutePolicyRepository routePolicyRepository;
	private final Cache<String, List<RoutePolicy>> routePolicyCache;

	public Mono<Boolean> isAllowed(String path, HttpMethod method, String role) {
		List<RoutePolicy> cached = routePolicyCache.getIfPresent(CACHE_KEY);

		if (cached != null) {
			return Mono.just(matches(cached, path, method, role));
		}

		return routePolicyRepository.findAllByEnabledTrue()
			.collectList()
			.doOnNext(list -> routePolicyCache.put(CACHE_KEY, list))
			.map(list -> matches(list, path, method, role));
	}

	private boolean matches(List<RoutePolicy> policies, String path, HttpMethod method, String role) {
		List<RoutePolicy> matchedPolicies = policies.stream()
			.filter(p -> p.getMethod().equalsIgnoreCase(method.name())
				&& PATH_MATCHER.match(p.getPathPattern(), path))
			.sorted((a, b) -> b.getPathPattern().length() - a.getPathPattern().length()) // 구체적인 패턴 우선
			.toList();

		if (matchedPolicies.isEmpty()) {
			return true;
		}

		RoutePolicy best = matchedPolicies.get(0);
		return Arrays.stream(best.getAllowedRoles().split(","))
			.map(String::trim)
			.anyMatch(r -> role != null && r.equalsIgnoreCase(role));
	}
}
