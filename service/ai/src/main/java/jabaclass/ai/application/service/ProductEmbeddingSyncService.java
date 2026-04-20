package jabaclass.ai.application.service;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jabaclass.ai.domain.repository.ProductEmbeddingRepository;
import jabaclass.ai.infrastructure.external.openai.EmbeddingService;
import jabaclass.ai.infrastructure.kafka.ProductAiSyncedEvent;
import jabaclass.ai.infrastructure.persistence.command.ProductEmbeddingUpsertCommand;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProductEmbeddingSyncService {

	private final EmbeddingService embeddingService;
	private final ProductEmbeddingRepository productEmbeddingRepository;

	@Transactional
	public void saveOrUpdate(ProductAiSyncedEvent payload) {
		float[] embedding = embeddingService.embedProductText(
			payload.title(),
			payload.description(),
			payload.roadAddress()
		);

		productEmbeddingRepository.upsert(
			new ProductEmbeddingUpsertCommand(
				payload.productId(),
				payload.title(),
				payload.description(),
				payload.price(),
				payload.roadAddress(),
				payload.status(),
				payload.popularity(),
				embedding
			)
		);
	}

	@Transactional
	public void delete(UUID productId) {
		productEmbeddingRepository.deleteByProductId(productId);
	}
}
