package jabaclass.apigateway.filter;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.jsonwebtoken.Claims;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import reactor.core.publisher.Mono;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;

import jabaclass.apigateway.exception.JwtAuthException;
import jabaclass.apigateway.exception.JwtErrorCode;
import jabaclass.apigateway.security.JwtProvider;
import jabaclass.apigateway.security.JwtTokenResolver;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {

    private final JwtProvider jwtProvider;
    private final JwtTokenResolver tokenResolver;
    private final ObjectMapper objectMapper;
    private final ReactiveStringRedisTemplate redisTemplate;

    private static final String BLACKLIST_PREFIX = "blacklist:";

    private record WhiteListEntry(HttpMethod method, String pattern) {}

    // 인증 없이 통과할 경로 (user-service의 공개 엔드포인트)
    private static final List<WhiteListEntry> WHITE_LIST = List.of(
        new WhiteListEntry(HttpMethod.POST,  "/api/v1/auth/login"),
        new WhiteListEntry(HttpMethod.POST,  "/api/v1/auth/reissue"),
        new WhiteListEntry(HttpMethod.POST,  "/api/v1/users/register"),
        new WhiteListEntry(HttpMethod.POST,  "/api/v1/users/email-check"),
        new WhiteListEntry(HttpMethod.POST,  "/api/v1/email/**"),
        new WhiteListEntry(HttpMethod.GET,   "/api/v1/products/**")
    );

    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();
        HttpMethod httpMethod = exchange.getRequest().getMethod();

        log.debug("[GATEWAY] Request: {} {}", httpMethod, path);

        if (httpMethod == null) {
            log.warn("[GATEWAY] Unknown HTTP method for path: {}", path);
            return onError(exchange, JwtErrorCode.INVALID_TOKEN);
        }

        if (isWhitelisted(path, httpMethod)) {
            log.info("[GATEWAY] White-listed path: {} {}", httpMethod, path);
            return chain.filter(exchange);
        }

        String token = tokenResolver.resolveToken(exchange);

        if (token == null) {
            log.warn("[GATEWAY] Token is missing for path: {} {}", httpMethod, path);
            return onError(exchange, JwtErrorCode.EMPTY_TOKEN);
        }

        // 성공 시 비동기 Redis 블랙리스트 조회로 연결
        try {
            Claims claims = jwtProvider.parseClaims(token);

            if (!jwtProvider.isAccessToken(claims)) {
                return onError(exchange, JwtErrorCode.INVALID_TOKEN);
            }

            return redisTemplate.hasKey(BLACKLIST_PREFIX + token)
                .flatMap(isBlacklisted -> {
                    if (isBlacklisted) {
                        log.warn("[GATEWAY] Blacklisted token. Path: {} {}", httpMethod, path);
                        return onError(exchange, JwtErrorCode.INVALID_TOKEN);
                    }

                    UUID userId = jwtProvider.getUserId(claims);
                    log.info("[GATEWAY] User authenticated. Path: {} {}, UserId: {}", httpMethod, path, userId);

                    ServerWebExchange mutatedExchange = exchange.mutate()
                        .request(r -> r.header("X-User-Id", userId.toString()))
                        .build();

                    return chain.filter(mutatedExchange);
                })
                .onErrorResume(e -> {
                    log.error("[GATEWAY] Redis error: {}", e.getMessage());
                    return onError(exchange, JwtErrorCode.INVALID_TOKEN);
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

    private boolean isWhitelisted(String path, HttpMethod method) {
        return WHITE_LIST.stream()
            .anyMatch(entry -> entry.method().equals(method)
                && PATH_MATCHER.match(entry.pattern(), path));
    }

    private Mono<Void> onError(ServerWebExchange exchange, JwtErrorCode errorCode) {
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
}
