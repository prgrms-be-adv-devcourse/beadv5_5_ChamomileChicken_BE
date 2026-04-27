package jabaclass.apigateway.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.util.AntPathMatcher;

import reactor.core.publisher.Mono;

import jabaclass.apigateway.config.GatewayRulesProperties;

@Slf4j
@Service
@RequiredArgsConstructor
public class WhitelistService {

	private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();
	private final GatewayRulesProperties properties;

	public Mono<Boolean> isWhitelisted(String path, HttpMethod method) {

		boolean matched = properties.whitelist().stream()
			.anyMatch(entry ->
				entry.method().equalsIgnoreCase(method.name())
				&& PATH_MATCHER.match(entry.path(), path)
			);
		return Mono.just(matched);
	}
}
