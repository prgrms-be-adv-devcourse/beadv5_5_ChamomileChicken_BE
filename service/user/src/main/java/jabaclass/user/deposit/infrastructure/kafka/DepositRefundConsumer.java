package jabaclass.user.deposit.infrastructure.kafka;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import jabaclass.user.common.kafka.OrderRefundedEvent;
import jabaclass.user.deposit.application.usecase.RefundDepositUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class DepositRefundConsumer {

	private final RefundDepositUseCase refundDepositUseCase;
	private final ObjectMapper objectMapper;

	@KafkaListener(
		topics = "order.refunded",
		groupId = "deposit-service"
	)
	public void consume(String message) {
		try {
			OrderRefundedEvent event = objectMapper.readValue(message, OrderRefundedEvent.class);

			log.info("예치금 복구 이벤트 수신 - orderId: {}, userId: {}, amount: {}",
				event.orderId(), event.userId(), event.depositAmount());

			refundDepositUseCase.refund(event.userId(), event.depositAmount());

		} catch (Exception e) {
			log.error("예치금 복구 실패 - message: {}, error: {}", message, e.getMessage());
			throw new RuntimeException(e); // 재시도를 위해 예외 던짐
		}
	}
}
