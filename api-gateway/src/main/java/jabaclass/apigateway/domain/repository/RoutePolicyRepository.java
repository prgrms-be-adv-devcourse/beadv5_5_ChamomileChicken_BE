package jabaclass.apigateway.domain.repository;

import java.util.UUID;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;

import jabaclass.apigateway.domain.model.RoutePolicy;
import reactor.core.publisher.Flux;

public interface RoutePolicyRepository extends ReactiveCrudRepository<RoutePolicy, UUID> {

	Flux<RoutePolicy> findAllByEnabledTrue();
}
