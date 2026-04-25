package jabaclass.user.user.infrastructure.kafka;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class UserEventsPublisher {

	public static final String USER_EVENTS_TOPIC = "user.events";

	private final KafkaTemplate<String, String> kafkaTemplate;
	private final ObjectMapper objectMapper;

	public void publishNameChanged(UUID userId, String newName) {
		try {
			String payload = objectMapper.writeValueAsString(new UserNameChangedEvent(userId, newName));
			ProducerRecord<String, String> record = new ProducerRecord<>(USER_EVENTS_TOPIC, userId.toString(), payload);
			record.headers().add(new RecordHeader("eventType", "USER_NAME_CHANGED".getBytes(StandardCharsets.UTF_8)));
			kafkaTemplate.send(record);
			log.info("USER_NAME_CHANGED 이벤트 발행: userId={}", userId);
		} catch (JsonProcessingException e) {
			log.error("USER_NAME_CHANGED 이벤트 직렬화 실패: userId={}", userId, e);
		}
	}

	public record UserNameChangedEvent(UUID userId, String newName) {}
}
