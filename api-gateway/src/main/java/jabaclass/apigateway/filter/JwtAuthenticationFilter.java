package jabaclass.apigateway.filter;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.cloud.client.circuitbreaker.ReactiveCircuitBreaker;
import org.springframework.cloud.client.circuitbreaker.ReactiveCircuitBreakerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.jsonwebtoken.Claims;

import reactor.core.publisher.Mono;

import jakarta.annotation.PostConstruct;

import jabaclass.apigateway.client.TokenStatusClient;
import jabaclass.apigateway.exception.RedisCircuitOpenException;
import jabaclass.apigateway.application.service.RbacService;
import jabaclass.apigateway.application.service.WhitelistService;
import jabaclass.apigateway.exception.AuthorizationServiceException;
import jabaclass.apigateway.exception.ErrorCode;
import jabaclass.apigateway.exception.JwtAuthException;
import jabaclass.apigateway.exception.JwtErrorCode;
import jabaclass.apigateway.exception.SystemErrorCode;
import jabaclass.apigateway.security.JwtProvider;
import jabaclass.apigateway.security.JwtTokenResolver;
import reactor.util.retry.Retry;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {

	private final JwtProvider jwtProvider;
	private final JwtTokenResolver tokenResolver;
	private final ObjectMapper objectMapper;
	private final ReactiveStringRedisTemplate redisTemplate;
	private final WhitelistService whitelistService;
	private final RbacService rbacService;
	private final ReactiveCircuitBreakerFactory<?, ?> circuitBreakerFactory;
	private final TokenStatusClient tokenStatusClient;
	private ReactiveCircuitBreaker redisCircuitBreaker;

	@PostConstruct
	void init() {
		this.redisCircuitBreaker = circuitBreakerFactory.create("redis-auth");
	}

    private static final String BLACKLIST_PREFIX = "blacklist:";
    private static final String FORCE_LOGOUT_PREFIX = "force_logout:";
	private static final Duration WHITELIST_TIMEOUT = Duration.ofSeconds(2);
	private static final Duration OPTIONAL_AUTH_ENRICHMENT_TIMEOUT = Duration.ofMillis(300);
	private static final Duration REQUIRED_AUTH_BLACKLIST_TIMEOUT = Duration.ofMillis(300);
	private static final Duration FORCED_LOGOUT_TIMEOUT = Duration.ofMillis(300);
	private static final Duration RBAC_TIMEOUT = Duration.ofSeconds(3);

	private static final byte[] FORBIDDEN_BODY =
		"{\"message\":\"접근 권한이 없습니다.\"}".getBytes(StandardCharsets.UTF_8);

	@Override
	public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
		ServerWebExchange sanitizedExchange = exchange.mutate()
			.request(r -> r.headers(headers -> {
				headers.remove("X-User-Id");
				headers.remove("X-User-Role");
			}))
			.build();

		String path = sanitizedExchange.getRequest().getURI().getPath();
		HttpMethod httpMethod = sanitizedExchange.getRequest().getMethod();

		if (httpMethod == null) {
			log.warn("[GATEWAY] Unknown HTTP method for path: {}", path);
			return onError(sanitizedExchange, JwtErrorCode.INVALID_TOKEN);
		}

		if (isPublicDocsPath(path, httpMethod)) {
			return chain.filter(sanitizedExchange);
		}

		long whitelistStart = System.currentTimeMillis();// 로그

		return whitelistService.isWhitelisted(path, httpMethod)
			//	.timeout(Duration.ofMillis(200))
			.timeout(WHITELIST_TIMEOUT)
			.doOnNext(
				r -> log.debug("[GATEWAY] whitelist check: {}ms", System.currentTimeMillis() - whitelistStart)) //로그
			.onErrorResume(e -> {
				log.error("[GATEWAY] Whitelist check failed, denying request: {}", e.getMessage());
				return Mono.just(false);
			})
			.flatMap(isWhitelisted -> {
				if (isWhitelisted) {
					return enrichIfAuthenticated(sanitizedExchange, chain, path, httpMethod);
				}
				return authenticate(sanitizedExchange, chain, path, httpMethod);
			});
	}

	private Mono<Void> enrichIfAuthenticated(ServerWebExchange exchange,
		GatewayFilterChain chain,
		String path,
		HttpMethod httpMethod) {
		String token = tokenResolver.resolveToken(exchange);
		if (token == null) {
			return chain.filter(exchange);
		}

		try {
			Claims claims = jwtProvider.parseClaims(token);

			if (!jwtProvider.isAccessToken(claims)) {
				log.debug("[GATEWAY] Ignore non-access token on whitelisted path: {} {}", httpMethod, path);
				return chain.filter(exchange);
			}

			UUID userId = jwtProvider.getUserId(claims);
			String role = jwtProvider.getRole(claims);

			return redisCircuitBreaker.run(
					redisTemplate.hasKey(BLACKLIST_PREFIX + token)
						.timeout(OPTIONAL_AUTH_ENRICHMENT_TIMEOUT)
						.retryWhen(Retry.fixedDelay(2, Duration.ofMillis(30))),
					throwable -> Mono.just(false)
				)
				.flatMap(isBlacklisted -> {
					if (Boolean.TRUE.equals(isBlacklisted)) {
						log.debug("[GATEWAY] Blacklisted token on whitelist path, skip enrichment: {} {}", httpMethod,
							path);
						return chain.filter(exchange);
					}
					ServerWebExchange mutatedExchange = exchange.mutate()
						.request(r -> {
							r.header("X-User-Id", userId.toString());
							if (role != null)
								r.header("X-User-Role", role);
						})
						.build();
					return chain.filter(mutatedExchange);
				})
				.onErrorResume(e -> {
					log.warn("[GATEWAY] Optional auth enrichment 실패: {} {}", httpMethod, path);
					return chain.filter(exchange);
				});
		} catch (Exception e) {
			log.debug("[GATEWAY] Invalid token on whitelisted path, skip enrichment: {} {}", httpMethod, path);
			return chain.filter(exchange);
		}
	}

	private Mono<Void> authenticate(ServerWebExchange exchange,
		GatewayFilterChain chain,
		String path,
		HttpMethod httpMethod) {

		String token = tokenResolver.resolveToken(exchange);

		if (token == null) {
			log.warn("[GATEWAY] Token is missing for path: {} {}", httpMethod, path);
			return onError(exchange, JwtErrorCode.EMPTY_TOKEN);
		}

		try {
			Claims claims = jwtProvider.parseClaims(token);

			if (!jwtProvider.isAccessToken(claims)) {
				return onError(exchange, JwtErrorCode.INVALID_TOKEN);
			}

			UUID userId = jwtProvider.getUserId(claims);
			String role = jwtProvider.getRole(claims);

			return redisCircuitBreaker.run(
					checkTokenViaRedis(token, userId, claims)
						.retryWhen(Retry.fixedDelay(1, Duration.ofMillis(50))),
					throwable -> {
						log.error("[GATEWAY] Redis 오류 발생 (UserService fallback): {}", extractMessage(throwable));
						return Mono.error(new RedisCircuitOpenException(throwable));
					}
				)
				.flatMap(result -> {
					if (result != TokenCheckResult.OK) {
						log.warn("[GATEWAY] Token rejected. result={}, path={} {}", result, httpMethod, path);
						return onError(exchange, JwtErrorCode.INVALID_TOKEN);
					}
					return proceedAfterAuth(exchange, chain, path, httpMethod, userId, role);
				})
				.onErrorResume(RedisCircuitOpenException.class, e -> {
					log.warn("[GATEWAY] Redis CB OPEN - UserService fallback. userId={}", userId);
					long iat = claims.getIssuedAt() != null ? claims.getIssuedAt().getTime() : 0L;
					return tokenStatusClient.check(token, userId, iat)
						.retryWhen(Retry.fixedDelay(1, Duration.ofMillis(100)))
						.flatMap(status -> {
							if (!status.tokenValid()) {
								log.warn("[GATEWAY] Fallback 차단. reason={}, userId={}", status.reason(), userId);
								return onError(exchange, JwtErrorCode.INVALID_TOKEN);
							}
							return proceedAfterAuth(exchange, chain, path, httpMethod, userId, role);
						})
						.onErrorResume(e2 -> {
							log.error("[GATEWAY] UserService fallback 실패. fail-closed. userId={}", userId);
							return onError(exchange, SystemErrorCode.AUTH_SERVICE_UNAVAILABLE);
						});
				})
				.onErrorResume(AuthorizationServiceException.class, e -> {
					log.error("[GATEWAY] RBAC 오류: {}", extractMessage(e));
					return onError(exchange, SystemErrorCode.INTERNAL_ERROR);
				})
				.onErrorResume(e -> {
					log.error("[GATEWAY] 예상치 못한 오류: {}", extractMessage(e));
					return onError(exchange, SystemErrorCode.INTERNAL_ERROR);
				});

		} catch (JwtAuthException e) {
			log.error("[GATEWAY] Auth Exception: {} {} - {}", httpMethod, path, e.getErrorCode().getMessage());
			return onError(exchange, e.getErrorCode());
		}
	}

	@Override
	public int getOrder() {
		return -1;
	}

	private boolean isPublicDocsPath(String path, HttpMethod method) {
		return HttpMethod.GET.equals(method)
			&& (path.equals("/swagger-ui")
			|| path.equals("/swagger-ui.html")
			|| path.startsWith("/swagger-ui/")
			|| path.startsWith("/v3/api-docs")
			|| path.startsWith("/docs/"));
	}

	private Mono<Void> onError(ServerWebExchange exchange, ErrorCode errorCode) {
		ServerHttpResponse response = exchange.getResponse();
		response.setStatusCode(errorCode.getStatus());
		response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

		try {
			byte[] bytes = objectMapper.writeValueAsBytes(
				Map.of("message", errorCode.getMessage())
			);
			DataBuffer buffer = response.bufferFactory().wrap(bytes);
			return response.writeWith(Mono.just(buffer));
		} catch (JsonProcessingException e) {
			log.error("[GATEWAY] Failed to serialize error response", e);
			return response.setComplete();
		}
	}

	private Mono<Void> onForbidden(ServerWebExchange exchange) {
		ServerHttpResponse response = exchange.getResponse();
		response.setStatusCode(HttpStatus.FORBIDDEN);
		response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
		DataBuffer buffer = response.bufferFactory().wrap(FORBIDDEN_BODY);
		return response.writeWith(Mono.just(buffer));
	}

	private String extractMessage(Throwable e) {
		return e.getCause() != null ? e.getCause().getMessage() : e.getMessage();
	}

	private enum TokenCheckResult {
		OK, BLACKLISTED, FORCE_LOGOUT
	}

	private Mono<TokenCheckResult> checkTokenViaRedis(String token, UUID userId, Claims claims) {
		return redisTemplate.hasKey(BLACKLIST_PREFIX + token)
			.timeout(REQUIRED_AUTH_BLACKLIST_TIMEOUT)
			.flatMap(isBlacklisted -> {
				if (Boolean.TRUE.equals(isBlacklisted)) {
					return Mono.just(TokenCheckResult.BLACKLISTED);
				}
				return redisTemplate.opsForValue().get(FORCE_LOGOUT_PREFIX + userId)
					.timeout(FORCED_LOGOUT_TIMEOUT)
					.defaultIfEmpty("")
					.map(forceLogoutTime -> {
						if (forceLogoutTime.isEmpty()) return TokenCheckResult.OK;
						try {
							long forceLogoutMillis = Long.parseLong(forceLogoutTime);
							long tokenIssuedAtMillis = claims.getIssuedAt() != null
								? claims.getIssuedAt().toInstant().toEpochMilli()
								: 0L;
							if (tokenIssuedAtMillis <= forceLogoutMillis) {
								return TokenCheckResult.FORCE_LOGOUT;
							}
						} catch (Exception e) {
							log.error("[GATEWAY] force_logout 파싱 실패. userId={}", userId);
						}
						return TokenCheckResult.OK;
					});
			});
	}

	private Mono<Void> proceedAfterAuth(ServerWebExchange exchange, GatewayFilterChain chain,
		String path, HttpMethod httpMethod, UUID userId, String role) {
		return rbacService.isAllowed(path, httpMethod, role)
			.timeout(RBAC_TIMEOUT)
			.onErrorMap(AuthorizationServiceException::new)
			.flatMap(isAllowed -> {
				if (!isAllowed) {
					log.warn("[GATEWAY] Role not allowed. path={} {}, role={}", httpMethod, path, role);
					return onForbidden(exchange);
				}
				ServerWebExchange mutated = exchange.mutate()
					.request(r -> {
						r.header("X-User-Id", userId.toString());
						if (role != null) r.header("X-User-Role", role);
					})
					.build();
				return chain.filter(mutated);
			});
	}
}
