package jabaclass.ai.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import java.math.BigDecimal;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import jabaclass.ai.domain.repository.ProductEmbeddingRepository;
import jabaclass.ai.domain.repository.RecommendationCacheRepository;
import jabaclass.ai.domain.repository.UserVectorCacheRepository;
import jabaclass.ai.infrastructure.external.openai.EmbeddingService;
import jabaclass.ai.infrastructure.kafka.ProductAiSyncedEvent;
import jabaclass.ai.infrastructure.persistence.command.ProductEmbeddingUpsertCommand;

@ExtendWith(MockitoExtension.class)
class ProductEmbeddingSyncServiceTest {

	@InjectMocks
	private ProductEmbeddingSyncService productEmbeddingSyncService;

	@Mock
	private EmbeddingService embeddingService;

	@Mock
	private ProductEmbeddingRepository productEmbeddingRepository;

	@Mock
	private UserVectorCacheRepository userVectorCacheRepository;

	@Mock
	private RecommendationCacheRepository recommendationCacheRepository;

	@Test
	void 상품_동기화시_임베딩을_생성하고_profile과_snapshot캐시를_무효화한다() {
		UUID eventId = UUID.fromString("11111111-2222-3333-4444-555555555555");
		UUID productId = UUID.fromString("66666666-7777-8888-9999-000000000000");
		ProductAiSyncedEvent payload = new ProductAiSyncedEvent(
			eventId,
			productId,
			"가죽 공예 클래스",
			"지갑 만들기",
			new BigDecimal("89000"),
			"서울 을지로",
			"ENABLE",
			12
		);
		float[] embedding = new float[] { 0.1f, 0.2f, 0.3f };
		given(embeddingService.embedProductText("가죽 공예 클래스", "지갑 만들기", "서울 을지로"))
			.willReturn(embedding);

		productEmbeddingSyncService.saveOrUpdate(payload);

		ArgumentCaptor<ProductEmbeddingUpsertCommand> captor =
			ArgumentCaptor.forClass(ProductEmbeddingUpsertCommand.class);
		then(productEmbeddingRepository).should().upsert(captor.capture());

		ProductEmbeddingUpsertCommand command = captor.getValue();
		assertThat(command.productId()).isEqualTo(productId);
		assertThat(command.title()).isEqualTo("가죽 공예 클래스");
		assertThat(command.description()).isEqualTo("지갑 만들기");
		assertThat(command.price()).isEqualByComparingTo("89000");
		assertThat(command.roadAddress()).isEqualTo("서울 을지로");
		assertThat(command.status()).isEqualTo("ENABLE");
		assertThat(command.popularity()).isEqualTo(12);
		assertThat(command.embedding()).isSameAs(embedding);
		then(userVectorCacheRepository).should().deleteAllProfiles();
		then(recommendationCacheRepository).should().deleteAll();
	}

	@Test
	void 상품_삭제시_profile과_snapshot캐시를_무효화한다() {
		UUID productId = UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee");

		productEmbeddingSyncService.delete(productId);

		then(productEmbeddingRepository).should().deleteByProductId(productId);
		then(userVectorCacheRepository).should().deleteAllProfiles();
		then(recommendationCacheRepository).should().deleteAll();
		then(embeddingService).shouldHaveNoInteractions();
	}
}
