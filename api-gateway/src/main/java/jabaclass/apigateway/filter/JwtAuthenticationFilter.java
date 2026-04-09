package jabaclass.apigateway.filter;

import io.jsonwebtoken.Claims;
import jabaclass.apigateway.exception.JwtAuthException;
import jabaclass.apigateway.exception.JwtErrorCode;
import jabaclass.apigateway.security.JwtProvider;
import jabaclass.apigateway.security.JwtTokenResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {

    private final JwtProvider jwtProvider;
    private final JwtTokenResolver tokenResolver;

    // 인증 없이 통과할 경로 (user-service의 공개 엔드포인트)
    private static final List<String> WHITE_LIST = List.of(
            "/api/v1/auth/login",
            "/api/v1/auth/reissue",
            "/api/v1/users/register",
            "/api/v1/users/email-check",
            "/api/v1/email/",
            "/api/v1/products"
    );

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();
        // 로그용
        String method = exchange.getRequest().getMethod().name();

        // 모든 요청의 진입점 기록
        log.info("[GATEWAY] Request: {} {}", method, path);

        if (isWhitelisted(path)) {
            log.info("[GATEWAY] White-listed path: {}", path); // 화이트리스트 통과
            return chain.filter(exchange);
        }

        String token = tokenResolver.resolveToken(exchange);

        if (token == null) {
            log.warn("[GATEWAY] Token is missing for path: {}", path); // 토큰 부재 경고
            return onError(exchange, JwtErrorCode.EMPTY_TOKEN);
        }

        try {
            Claims claims = jwtProvider.parseClaims(token);

            if (!jwtProvider.isAccessToken(claims)) {
                return onError(exchange, JwtErrorCode.INVALID_TOKEN);
            }

            UUID userId = jwtProvider.getUserId(claims);

            // 인증 성공 및 유저 식별 정보
            log.info("[GATEWAY] User authenticated. Path: {}, UserId: {}", path, userId);

            // 검증된 userId를 헤더에 담아 downstream 서비스로 전달
            ServerWebExchange mutatedExchange = exchange.mutate()
                    .request(r -> r.header("X-User-Id", userId.toString()))
                    .build();

            return chain.filter(mutatedExchange);

        } catch (JwtAuthException e) {
            log.error("[GATEWAY] Auth Exception: {}", e.getErrorCode().getMessage()); // [추천 로그 5] 인증 실패 에러
            return onError(exchange, e.getErrorCode());
        }
    }

    @Override
    public int getOrder() {
        return -1; // 모든 Gateway 필터보다 먼저 실행
    }

    private boolean isWhitelisted(String path) {
        return WHITE_LIST.stream().anyMatch(path::startsWith);
    }

    private Mono<Void> onError(ServerWebExchange exchange, JwtErrorCode errorCode) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(errorCode.getStatus());
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        String body = "{\"message\":\"" + errorCode.getMessage() + "\"}";
        DataBuffer buffer = response.bufferFactory()
                .wrap(body.getBytes(StandardCharsets.UTF_8));

        return response.writeWith(Mono.just(buffer));
    }
}
