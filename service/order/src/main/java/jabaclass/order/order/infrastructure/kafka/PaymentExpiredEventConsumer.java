package jabaclass.order.order.infrastructure.kafka;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;

import jabaclass.order.order.domain.model.Order;
import jabaclass.order.order.domain.model.OrderStatus;
import jabaclass.order.order.domain.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentExpiredEventConsumer {
	private final OrderRepository orderRepository;
	private final ObjectMapper objectMapper;

	@KafkaListener(
		topics = "payment.expired",
		groupId = "order-service"
	)
	@Transactional
	public void consume(String message) {

		try {
			// 1. JSON → 객체 변환
			PaymentExpiredEvent event =
				objectMapper.readValue(message, PaymentExpiredEvent.class);

			log.info("payment.expired 이벤트 수신. orderId={}", event.orderId());

			// 2. Order 조회
			Order order = orderRepository.findById(event.orderId())
				.orElseThrow(() ->
					new IllegalArgumentException("주문 없음: " + event.orderId())
				);

			// 3. 중복 처리 방지
			if (order.getStatus() == OrderStatus.EXPIRED) {
				log.info("이미 만료된 주문. orderId={}", event.orderId());
				return;
			}

			// 4. 상태 변경
			order.expire();

			log.info("주문 만료 완료. orderId={}", event.orderId());

		} catch (Exception e) {
			log.error("payment.expired 처리 실패. message={}", message, e);
			throw new RuntimeException(e); // Kafka 재시도 유도
		}
	}
}
