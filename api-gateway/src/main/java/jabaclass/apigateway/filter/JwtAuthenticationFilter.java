package jabaclass.apigateway.filter;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeoutException;

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

import io.jsonwebtoken.Claims;
import jabaclass.apigateway.application.service.RbacService;
import jabaclass.apigateway.application.service.WhitelistService;
import jabaclass.apigateway.exception.AuthorizationServiceException;
import jabaclass.apigateway.exception.ErrorCode;
import jabaclass.apigateway.exception.JwtAuthException;
import jabaclass.apigateway.exception.JwtErrorCode;
import jabaclass.apigateway.exception.RedisBlacklistException;
import jabaclass.apigateway.exception.SystemErrorCode;
import jabaclass.apigateway.security.JwtProvider;
import jabaclass.apigateway.security.JwtTokenResolver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

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

	private static final String BLACKLIST_PREFIX = "blacklist:";

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

		long whitelistStart = System.currentTimeMillis();// 로그

		return whitelistService.isWhitelisted(path, httpMethod)
			//	.timeout(Duration.ofMillis(200))
			.timeout(Duration.ofSeconds(2))
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

			long blacklistStart = System.currentTimeMillis(); // 로그

			return redisTemplate.hasKey(BLACKLIST_PREFIX + token)
				//.timeout(Duration.ofMillis(200))
				.timeout(Duration.ofSeconds(2))
				.doOnNext(r -> log.debug("[GATEWAY] blacklist check: {}ms",
					System.currentTimeMillis() - blacklistStart)) // 로그
				.flatMap(isBlacklisted -> {
					if (isBlacklisted) {
						log.debug("[GATEWAY] Ignore blacklisted token on whitelisted path: {} {}", httpMethod, path);
						return chain.filter(exchange);
					}

					ServerWebExchange mutatedExchange = exchange.mutate()
						.request(r -> {
							r.header("X-User-Id", userId.toString());
							if (role != null) {
								r.header("X-User-Role", role);
							}
						})
						.build();

					return chain.filter(mutatedExchange);
				})
				.onErrorResume(TimeoutException.class, e -> {
					log.warn("[GATEWAY] Optional auth enrichment timed out, continuing without user context: {} {}",
						httpMethod,
						path);
					return chain.filter(exchange);
				})
				.onErrorResume(e -> {
					log.warn("[GATEWAY] Optional auth enrichment failed, continuing without user context: {} {}",
						httpMethod,
						path, e);
					return chain.filter(exchange);
				});
		} catch (Exception e) {
			log.debug("[GATEWAY] Ignore invalid token on whitelisted path: {} {}", httpMethod, path);
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

			return redisTemplate.hasKey(BLACKLIST_PREFIX + token)
				//	.timeout(Duration.ofMillis(200))
				.timeout(Duration.ofSeconds(2))
				.onErrorMap(RedisBlacklistException::new)
				.flatMap(isBlacklisted -> {
					if (isBlacklisted) {
						log.warn("[GATEWAY] Blacklisted token. Path: {} {}", httpMethod, path);
						return onError(exchange, JwtErrorCode.INVALID_TOKEN);
					}

					UUID userId = jwtProvider.getUserId(claims);
					String role = jwtProvider.getRole(claims);

					return rbacService.isAllowed(path, httpMethod, role)
						//	.timeout(Duration.ofMillis(300))
						.timeout(Duration.ofSeconds(3))
						.onErrorMap(AuthorizationServiceException::new)
						.flatMap(isAllowed -> {
							if (!isAllowed) {
								log.warn("[GATEWAY] Role not allowed. Path: {} {}, Role: {}", httpMethod, path, role);
								return onForbidden(exchange);
							}

							ServerWebExchange mutatedExchange = exchange.mutate()
								.request(r -> {
									r.header("X-User-Id", userId.toString());
									if (role != null) {
										r.header("X-User-Role", role);
									}
								})
								.build();

							return chain.filter(mutatedExchange);
						});
				})
				.onErrorResume(RedisBlacklistException.class, e -> {
					log.error("[GATEWAY] Redis blacklist check failed: {}", extractMessage(e));
					return onError(exchange, SystemErrorCode.AUTH_SERVICE_UNAVAILABLE);
				})
				.onErrorResume(AuthorizationServiceException.class, e -> {
					log.error("[GATEWAY] RBAC system error: {}", extractMessage(e));
					return onError(exchange, SystemErrorCode.INTERNAL_ERROR);
				})
				.onErrorResume(e -> {
					log.error("[GATEWAY] Unexpected error: {}", extractMessage(e));
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
}
