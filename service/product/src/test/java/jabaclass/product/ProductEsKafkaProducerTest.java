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
import org.springframework.kafka.core.KafkaTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import jabaclass.product.infrastructure.elasticsearch.ProductDocument;
import jabaclass.product.infrastructure.event.dto.ProductEsDeleteEvent;
import jabaclass.product.infrastructure.event.dto.ProductEsSaveEvent;
import jabaclass.product.infrastructure.kafka.ProductEsKafkaProducer;

@ExtendWith(MockitoExtension.class)
class ProductEsKafkaProducerTest {

	@InjectMocks
	private ProductEsKafkaProducer producer;

	@Mock
	private KafkaTemplate<String, String> kafkaTemplate;

	private ObjectMapper objectMapper;

	private static final String PRODUCT_ID = UUID.randomUUID().toString();

	@BeforeEach
	void setUp() throws Exception {
		objectMapper = new ObjectMapper();
		objectMapper.registerModule(new JavaTimeModule());
		objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

		java.lang.reflect.Field field = ProductEsKafkaProducer.class.getDeclaredField("objectMapper");
		field.setAccessible(true);
		field.set(producer, objectMapper);
	}

	@Test
	void SAVE_이벤트_수신시_Kafka_토픽으로_메시지를_발행한다() {
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

		ProductEsSaveEvent event = new ProductEsSaveEvent(document);
		producer.handleSave(event);

		then(kafkaTemplate).should().send(eq(ProductEsKafkaProducer.TOPIC), anyString());
	}

	@Test
	void DELETE_이벤트_수신시_Kafka_토픽으로_메시지를_발행한다() {
		ProductEsDeleteEvent event = new ProductEsDeleteEvent(PRODUCT_ID);
		producer.handleDelete(event);

		then(kafkaTemplate).should().send(eq(ProductEsKafkaProducer.TOPIC), anyString());
	}

	@Test
	void kafkaTemplate_예외_발생시_RuntimeException_없이_로그만_남긴다() {
		ProductEsDeleteEvent event = new ProductEsDeleteEvent(PRODUCT_ID);
		given(kafkaTemplate.send(anyString(), anyString())).willThrow(new RuntimeException("Kafka 연결 실패"));

		// 예외가 외부로 전파되지 않아야 함
		assertThatCode(() -> producer.handleDelete(event))
			.doesNotThrowAnyException();
	}
}