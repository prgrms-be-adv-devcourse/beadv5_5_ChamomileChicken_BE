package jabaclass.order.common.config;

import java.util.HashMap;
import java.util.Map;

import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;

@Configuration
public class KafkaProducerConfig {

	@Value("${spring.kafka.bootstrap-servers}")
	private String bootstrapServers;

	@Bean
	public ProducerFactory<String, String> producerFactory() {
		Map<String, Object> config = new HashMap<>();
		config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
		config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
		config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
		config.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true); // 브로커 수준 중복 발행 방지 (acks=all, retries=MAX_VALUE, max.in.flight=5 자동 강제)
		config.put(ProducerConfig.ACKS_CONFIG, "all"); // 리더 + 모든 ISR 복제 확인 후 성공 처리 — 브로커 장애 시 메시지 유실 방지
		config.put(ProducerConfig.RETRIES_CONFIG, Integer.MAX_VALUE); // 일시적 브로커 오류 시 무한 재시도 — DELIVERY_TIMEOUT_MS 내에서만 동작
		config.put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, 5); // idempotence 활성화 시 최대 5까지 순서 보장 — 1로 낮추면 순서 완벽하지만 처리량 감소
		config.put(ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG, 120_000); // send 호출 후 최대 2분 내 미전달 시 TimeoutException → Outbox catch 블록에서 retry 처리
		return new DefaultKafkaProducerFactory<>(config);
	}

	@Bean
	public KafkaTemplate<String, String> kafkaTemplate() {
		return new KafkaTemplate<>(producerFactory());
	}
}
