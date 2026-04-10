package jabaclass.apigateway.security;

import org.springframework.http.HttpHeaders;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;

public class JwtTokenResolver {

    private static final String BEARER_PREFIX = "Bearer ";

    /**
     * Authorization 헤더에서 accessToken 추출.
     * Vue.js는 API 요청 시 Authorization: Bearer {accessToken} 헤더로 전송.
     */
    public String resolveToken(ServerWebExchange exchange) {
        String bearerToken = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

        if (!StringUtils.hasText(bearerToken) || !bearerToken.startsWith(BEARER_PREFIX)) {
            return null;
        }

        String token = bearerToken.substring(BEARER_PREFIX.length()).trim();
        return StringUtils.hasText(token) ? token : null;
    }
}
