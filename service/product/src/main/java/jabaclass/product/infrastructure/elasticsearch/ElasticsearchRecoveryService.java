package jabaclass.product.infrastructure.elasticsearch;

import jabaclass.product.infrastructure.outbox.OutboxService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ElasticsearchRecoveryService {

	private final OutboxService outboxService;

	@Async
	public void retryFailedEsEvents() {
		int count = outboxService.resetFailedEsEvents();
		if (count > 0) {
			log.info("ES 복구 감지 — FAILED 아웃박스 이벤트 {}건 재처리 예약", count);
		}
	}
}
