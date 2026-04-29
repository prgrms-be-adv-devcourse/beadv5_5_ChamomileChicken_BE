package jabaclass.product.infrastructure.elasticsearch;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ElasticsearchCircuitBreakerEventListener {

	private final CircuitBreakerRegistry circuitBreakerRegistry;
	private final ElasticsearchRecoveryService recoveryService;

	@PostConstruct
	public void registerListener() {
		circuitBreakerRegistry.circuitBreaker("elasticsearchCB")
			.getEventPublisher()
			.onStateTransition(event -> {
				if (event.getStateTransition().getToState() == CircuitBreaker.State.CLOSED) {
					recoveryService.retryFailedEsEvents(); // 외부 빈 호출 → 프록시 경유 → @Async 정상 동작
				}
			});
	}
}
