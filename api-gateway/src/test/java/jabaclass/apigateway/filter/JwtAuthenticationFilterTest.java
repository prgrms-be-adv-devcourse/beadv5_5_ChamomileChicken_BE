package jabaclass.apigateway.filter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.jsonwebtoken.Claims;

import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import jabaclass.apigateway.application.service.RbacService;
import jabaclass.apigateway.application.service.WhitelistService;
import jabaclass.apigateway.security.JwtProvider;
import jabaclass.apigateway.security.JwtTokenResolver;

class JwtAuthenticationFilterTest {

    @Test
    void continuesRequestWhenOptionalAuthEnrichmentTimesOut() {
        JwtProvider jwtProvider = mock(JwtProvider.class);
        JwtTokenResolver tokenResolver = mock(JwtTokenResolver.class);
        ReactiveStringRedisTemplate redisTemplate = mock(ReactiveStringRedisTemplate.class);
        WhitelistService whitelistService = mock(WhitelistService.class);
        RbacService rbacService = mock(RbacService.class);
        Claims claims = mock(Claims.class);

        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(
            jwtProvider,
            tokenResolver,
            new ObjectMapper(),
            redisTemplate,
            whitelistService,
            rbacService
        );

        MockServerWebExchange exchange = MockServerWebExchange.from(
            MockServerHttpRequest.get("/public/test")
                .header(HttpHeaders.AUTHORIZATION, "Bearer access-token")
                .build()
        );

        when(whitelistService.isWhitelisted("/public/test", HttpMethod.GET)).thenReturn(Mono.just(true));
        when(tokenResolver.resolveToken(any(ServerWebExchange.class))).thenReturn("access-token");
        when(jwtProvider.parseClaims("access-token")).thenReturn(claims);
        when(jwtProvider.isAccessToken(claims)).thenReturn(true);
        when(jwtProvider.getRole(claims)).thenReturn("USER");
        when(jwtProvider.getUserId(claims)).thenReturn(java.util.UUID.fromString("11111111-1111-1111-1111-111111111111"));
        when(redisTemplate.hasKey("blacklist:access-token"))
            .thenReturn(Mono.error(new TimeoutException("optional auth enrichment timeout")));

        AtomicReference<ServerHttpRequest> forwardedRequest = new AtomicReference<>();
        GatewayFilterChain chain = chainExchange -> {
            forwardedRequest.set(chainExchange.getRequest());
            return Mono.empty();
        };

        StepVerifier.create(filter.filter(exchange, chain))
            .verifyComplete();

        verify(redisTemplate).hasKey("blacklist:access-token");
        assertThat(forwardedRequest.get()).isNotNull();
        assertThat(forwardedRequest.get().getHeaders().containsKey("X-User-Id")).isFalse();
        assertThat(forwardedRequest.get().getHeaders().containsKey("X-User-Role")).isFalse();
    }
}
