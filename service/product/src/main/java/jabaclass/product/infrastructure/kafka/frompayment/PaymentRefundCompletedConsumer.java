package jabaclass.product.infrastructure.kafka.frompayment;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import jabaclass.product.application.usecase.ScheduleUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentRefundCompletedConsumer {

	public static final String TOPIC = "payment.refund.completed";

	private final ScheduleUseCase scheduleUseCase;
	private final ObjectMapper objectMapper;

	@KafkaListener(
		topics = TOPIC,
		groupId = "product-service"
	)
	public void consume(String message) {
		// TODO: 환불 구현 예정
		// 주의: payment.refund.completed 페이로드는 orderId만 포함
		// ProductService는 productUserId가 필요하므로 아래 중 하나로 구현 필요:
		//   방안 A. Order가 환불 처리 후 order.reservation.refunded { productUserId } 발행 → Product가 수신
		//   방안 B. 이 컨슈머에서 Order 서비스에 productUserId 조회 후 처리
		// 구현 시 방안 결정 후 토픽 및 이벤트 구조 재설계 필요

		try {
			PaymentRefundCompletedEvent event = objectMapper.readValue(message, PaymentRefundCompletedEvent.class);
			log.info("payment.refund.completed 수신. productUserId={} (환불 미구현)", event.productUserId());

			// scheduleUseCase.refundReservation(event.productUserId());
		} catch (Exception e) {
			log.error("payment.refund.completed 처리 실패. message={}", message, e);
			throw new RuntimeException("상품 환불 이벤트 처리 실패", e);
		}
	}
}