package jabaclass.apigateway.domain.repository;

import java.util.UUID;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;

import jabaclass.apigateway.domain.model.WhitelistEntry;
import reactor.core.publisher.Flux;

public interface WhitelistRepository extends ReactiveCrudRepository<WhitelistEntry, UUID> {

	Flux<WhitelistEntry> findAllByEnabledTrue();
}
