package jabaclass.product;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

import java.math.BigDecimal;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import jabaclass.product.domain.repository.ProductSearchRepository;
import jabaclass.product.infrastructure.elasticsearch.ProductDocument;
import jabaclass.product.infrastructure.kafka.ProductEsIndexMessage;
import jabaclass.product.infrastructure.kafka.ProductEsKafkaConsumer;

@ExtendWith(MockitoExtension.class)
class ProductEsKafkaConsumerTest {

	@InjectMocks
	private ProductEsKafkaConsumer consumer;

	@Mock
	private ProductSearchRepository productSearchRepository;

	private ObjectMapper objectMapper;

	private static final String PRODUCT_ID = UUID.randomUUID().toString();

	@BeforeEach
	void setUp() throws Exception {
		objectMapper = new ObjectMapper();
		objectMapper.registerModule(new JavaTimeModule());
		objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

		// @InjectMocks로 생성된 consumer에 objectMapper 직접 주입
		java.lang.reflect.Field field = ProductEsKafkaConsumer.class.getDeclaredField("objectMapper");
		field.setAccessible(true);
		field.set(consumer, objectMapper);
	}

	@Test
	void SAVE_메시지_수신시_ES에_저장한다() throws Exception {
		ProductDocument document = ProductDocument.builder()
			.id(PRODUCT_ID)
			.sellerId(UUID.randomUUID().toString())
			.sellerName("판매자A")
			.title("향수 공방 클래스")
			.description("직접 향수를 만들어보는 클래스입니다.")
			.status("ENABLE")
			.price(new BigDecimal("50000"))
			.maxCapacity(10)
			.deleted(false)
			.build();

		ProductEsIndexMessage message = ProductEsIndexMessage.save(document);
		String payload = objectMapper.writeValueAsString(message);

		consumer.consume(payload);

		then(productSearchRepository).should().save(any(ProductDocument.class));
		then(productSearchRepository).should(never()).deleteById(any());
	}

	@Test
	void DELETE_메시지_수신시_ES에서_삭제한다() throws Exception {
		ProductEsIndexMessage message = ProductEsIndexMessage.delete(PRODUCT_ID);
		String payload = objectMapper.writeValueAsString(message);

		consumer.consume(payload);

		then(productSearchRepository).should().deleteById(PRODUCT_ID);
		then(productSearchRepository).should(never()).save(any());
	}

	@Test
	void 알수없는_operation_수신시_ES_작업을_수행하지_않는다() throws Exception {
		ProductEsIndexMessage message = new ProductEsIndexMessage("UNKNOWN", null, PRODUCT_ID);
		String payload = objectMapper.writeValueAsString(message);

		// 예외 없이 처리되어야 함
		assertThatCode(() -> consumer.consume(payload))
			.doesNotThrowAnyException();

		then(productSearchRepository).shouldHaveNoInteractions();
	}

	@Test
	void 잘못된_JSON_수신시_RuntimeException을_던진다() {
		String invalidPayload = "{ invalid json }";

		assertThatThrownBy(() -> consumer.consume(invalidPayload))
			.isInstanceOf(RuntimeException.class)
			.hasMessageContaining("ES 색인 이벤트 처리 실패");
	}
}