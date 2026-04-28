package jabaclass.ai.application.service;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import jabaclass.ai.domain.repository.ProductEmbeddingRepository;
import jabaclass.ai.domain.repository.RecommendationCacheRepository;
import jabaclass.ai.domain.repository.UserVectorCacheRepository;
import jabaclass.ai.infrastructure.external.openai.EmbeddingService;
import jabaclass.ai.infrastructure.kafka.ProductAiSyncedEvent;
import jabaclass.ai.infrastructure.persistence.command.ProductEmbeddingUpsertCommand;
import lombok.RequiredArgsConstructor;

/**
 * 상품 정보를 OpenAI 임베딩으로 바꿔 product_embeddings에 저장/갱신하는 서비스
 */
@Service
@RequiredArgsConstructor
public class ProductEmbeddingSyncService {

	private final EmbeddingService embeddingService;
	private final ProductEmbeddingRepository productEmbeddingRepository;
	private final UserVectorCacheRepository userVectorCacheRepository;
	private final RecommendationCacheRepository recommendationCacheRepository;

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
		invalidateRecommendationCachesAfterCommit();
	}

	@Transactional
	public void delete(UUID productId) {
		productEmbeddingRepository.deleteByProductId(productId);
		invalidateRecommendationCachesAfterCommit();
	}

	private void invalidateRecommendationCachesAfterCommit() {
		Runnable invalidateTask = () -> {
			userVectorCacheRepository.deleteAllProfiles();
			recommendationCacheRepository.deleteAll();
		};

		if (!TransactionSynchronizationManager.isSynchronizationActive()) {
			invalidateTask.run();
			return;
		}

		TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
			@Override
			public void afterCommit() {
				invalidateTask.run();
			}
		});
	}
}
