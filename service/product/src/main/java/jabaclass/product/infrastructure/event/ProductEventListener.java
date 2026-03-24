package jabaclass.product.infrastructure.event;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import jabaclass.product.infrastructure.event.dto.ProductEventResposeDto;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class ProductEventListener {

	// TODO 파이널에 kafka로 대체
	// DB 커밋 후 발
	@Async
	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void handle(ProductEventResposeDto product) {
		log.info("상품 저장 완료: {}", product.productId());
		// 캐시 갱신
		// 알림 발송
	}

}
