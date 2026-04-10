package jabaclass.product.infrastructure.kafka;

import org.springframework.kafka.annotation.BackOff;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import jabaclass.product.application.usecase.ProductUseCase;
import jabaclass.product.application.usecase.ProductUserUseCase;
import jabaclass.product.application.usecase.ScheduleUseCase;
import jabaclass.product.domain.model.Product;
import jabaclass.product.domain.model.ProductUser;
import jabaclass.product.domain.model.Schedule;
import jabaclass.product.domain.model.status.OrderStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentRefundCompletedConsumer {

	private final ScheduleUseCase scheduleUseCase;
	private final ObjectMapper objectMapper;
	private final ProductUserUseCase productUserUseCase;
	private final ProductUseCase productUseCase;

	@RetryableTopic(
		attempts = "5", // 총 5번 시도
		backOff = @BackOff(delay = 5000) // 5초 간격
	)
	@KafkaListener(
		topics = "payment.refund.completed",
		groupId = "product-service"
	)
	public void consume(String message, Acknowledgment ack) {
		try {
			PaymentRefundCompletedEvent event = objectMapper.readValue(message, PaymentRefundCompletedEvent.class);
			// 멱등성 확인 및 선점
			int claimRestore = scheduleUseCase.claimRestore(event.productUserId(), OrderStatus.RESTORING);
			// 중복인 경우
			if (claimRestore == 0)
				ack.acknowledge(); // 정상 처리

			Schedule schedule = scheduleUseCase.findByProductUserId(event.productUserId());
			Product product = productUseCase.findByIdOrThrow(schedule.getProductId());
			ProductUser productUser = productUserUseCase.innerFindById(event.productUserId());

			int restoreCapacity = 0;
			// 상태값 변경 및 재고 복구
			switch (event.status()) {
				// 취소,실패
				case CANCELLED:
					restoreCapacity = scheduleUseCase.restoreCapacity(schedule.getId(), productUser.getGuestCount(),
						product.getMaxCapacity());
					// 재고 복구 실패시
					if (restoreCapacity == 0)
						throw new RuntimeException("재고 복구 실패");

					// 상태값 변경
					scheduleUseCase.restoringInventory(productUser.getId(), OrderStatus.EXPIRED);
					break;

				// 환불
				case REFUNDED:
					restoreCapacity = scheduleUseCase.restoreCapacity(schedule.getId(), productUser.getGuestCount(),
						product.getMaxCapacity());
					// 재고 복구 실패시
					if (restoreCapacity == 0)
						throw new RuntimeException("재고 복구 실패");

					// 상태값 변경
					scheduleUseCase.restoringInventory(productUser.getId(), OrderStatus.REFUNDED);
					break;

				// 결제 성공
				case SUCCESS:
					restoreCapacity = scheduleUseCase.restoreCapacity(schedule.getId(), productUser.getGuestCount(),
						product.getMaxCapacity());
					// 상태값 변경
					scheduleUseCase.restoringInventory(productUser.getId(), OrderStatus.PAID);
					break;
			}
		} catch (Exception e) {
			// 일시적 장애 가능성 있음
			// 이 경우만 예외를 다시 던져서 재시도
			throw new RuntimeException("상품 환불 이벤트 처리 실패", e);
		}
	}
}
