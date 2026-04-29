package jabaclass.apigateway.client;

import java.time.Duration;
import java.util.UUID;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import reactor.core.publisher.Mono;

import jabaclass.apigateway.dto.TokenStatusResult;

@Component
@Slf4j
public class TokenStatusClient {

	private final WebClient webClient;
	private final String internalSecret;

	public TokenStatusClient(
		WebClient.Builder webClientBuilder,
		@Value("${internal.user-service-url}") String userServiceUrl,
		@Value("${internal.secret}") String internalSecret
	) {
		this.webClient = webClientBuilder
			.baseUrl(userServiceUrl)
			.build();
		this.internalSecret = internalSecret;
	}

	public Mono<TokenStatusResult> check(String token, UUID userId, long issuedAtMillis) {
		return webClient.get()
			.uri("/api/v1/auth/internal/token-status")
			.header("X-Token", token)
			.header("X-User-Id", userId.toString())
			.header("X-Token-Iat", String.valueOf(issuedAtMillis))
			.header("X-Internal-Secret", internalSecret)
			.retrieve()
			.bodyToMono(TokenStatusResult.class)
			.timeout(Duration.ofSeconds(3))
			.doOnError(e -> log.error("[GATEWAY] UserService token-status 호출 실패: {}", e.getMessage()));
	}
}
