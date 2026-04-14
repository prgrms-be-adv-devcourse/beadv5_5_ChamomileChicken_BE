package jabaclass.product.application.scheduled;

import java.time.LocalDate;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import jabaclass.product.application.usecase.ScheduleUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class ScheduleStatusScheduler {
	private final ScheduleUseCase scheduleUseCase;
	private static final int BATCH_SIZE = 100;

	// 초 분 시 일 월 요일
	@Scheduled(cron = "0 5 9 * * *")
	public void closeExpiredSchedules() {
		LocalDate today = LocalDate.now();
		int totalUpdated = 0;

		while (true) {
			int updated = scheduleUseCase.closeExpiredSchedulesOnce();
			totalUpdated += updated;

			if (updated < BATCH_SIZE) {
				break;
			}
		}

		log.info("날짜 마감 처리 완료 - totalUpdated={}, today={}", totalUpdated, today);
	}

}
