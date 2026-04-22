package jabaclass.apigateway.filter;

import java.util.Optional;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;

import reactor.core.publisher.Mono;

@Component
@Order(-2)
public class IpForwardingFilter implements WebFilter {

	@Override
	public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
		String clientIp = Optional.ofNullable(exchange.getRequest().getRemoteAddress())
			.map(addr -> addr.getAddress().getHostAddress())
			.orElse("unknown");

		ServerWebExchange mutated = exchange.mutate()
			.request(r -> r.headers(headers -> {
				headers.remove("X-Real-IP");
				headers.set("X-Real-IP", clientIp);
			}))
			.build();

		return chain.filter(mutated);
	}
}
