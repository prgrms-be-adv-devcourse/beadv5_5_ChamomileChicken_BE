package jabaclass.ai.domain.repository;

import java.util.UUID;

import jabaclass.ai.infrastructure.persistence.command.ProductEmbeddingUpsertCommand;

public interface ProductEmbeddingRepository {

	float[] findEmbeddingByProductId(UUID productId);

	void upsert(ProductEmbeddingUpsertCommand command);

	void deleteByProductId(UUID productId);
}
