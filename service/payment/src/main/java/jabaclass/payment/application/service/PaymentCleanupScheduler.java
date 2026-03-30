package jabaclass.payment.application.service;

import jabaclass.payment.application.usecase.ExpirePaymentUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentCleanupScheduler {

	private final ExpirePaymentUseCase expirePaymentUseCase;

	@Scheduled(cron = "0 */5 * * * *")
	public void cleanupExpiredPayments() {
		log.info("결제 만료 스케줄러 시작");
		expirePaymentUseCase.execute();
		log.info("결제 만료 스케줄러 종료");
	}
}