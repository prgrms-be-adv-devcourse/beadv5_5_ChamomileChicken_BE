package jabaclass.ai.domain.repository;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;

import jabaclass.ai.infrastructure.persistence.command.ProductEmbeddingUpsertCommand;

public interface ProductEmbeddingRepository {

	Map<UUID, float[]> findAllByProductIds(Collection<UUID> productIds);

	void upsert(ProductEmbeddingUpsertCommand command);

	void deleteByProductId(UUID productId);
}
