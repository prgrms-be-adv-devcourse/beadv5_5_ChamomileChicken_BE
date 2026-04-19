package jabaclass.ai.domain.repository;

import java.util.UUID;

public interface ProductEmbeddingRepository {

	float[] findEmbeddingByProductId(UUID productId);
}
