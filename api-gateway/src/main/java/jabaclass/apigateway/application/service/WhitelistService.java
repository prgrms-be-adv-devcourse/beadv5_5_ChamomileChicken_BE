package jabaclass.apigateway.application.service;

import java.util.List;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.util.AntPathMatcher;

import com.github.benmanes.caffeine.cache.Cache;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

import jabaclass.apigateway.domain.model.WhitelistEntry;
import jabaclass.apigateway.domain.repository.WhitelistRepository;

@Slf4j
@Service
@RequiredArgsConstructor
public class WhitelistService {

	private static final String CACHE_KEY = "whitelist";
	private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

	private final WhitelistRepository whitelistRepository;
	private final Cache<String, List<WhitelistEntry>> whitelistCache;

	public Mono<Boolean> isWhitelisted(String path, HttpMethod method) {
		List<WhitelistEntry> cached = whitelistCache.getIfPresent(CACHE_KEY);

		if (cached != null) {
			return Mono.just(matches(cached, path, method));
		}

		return whitelistRepository.findAllByEnabledTrue()
			.collectList()
			.doOnNext(list -> {
				log.debug("[GATEWAY] Whitelist loaded from DB: {} entries", list.size());
				whitelistCache.put(CACHE_KEY, list);
			})
			.map(list -> matches(list, path, method));
	}

	private boolean matches(List<WhitelistEntry> entries, String path, HttpMethod method) {
		return entries.stream().anyMatch(entry ->
			entry.getMethod().equalsIgnoreCase(method.name())
				&& PATH_MATCHER.match(entry.getPathPattern(), path)
		);
	}
}
