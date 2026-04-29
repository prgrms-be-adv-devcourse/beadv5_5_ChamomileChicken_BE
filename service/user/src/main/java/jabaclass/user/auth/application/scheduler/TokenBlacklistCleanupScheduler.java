package jabaclass.user.auth.application.scheduler;

import java.time.LocalDateTime;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import jabaclass.user.auth.domain.repository.TokenBlacklistRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class TokenBlacklistCleanupScheduler {

	private final TokenBlacklistRepository tokenBlacklistRepository;

	@Scheduled(cron = "0 0 3 * * *", zone = "Asia/Seoul")
	@Transactional
	public void cleanupExpiredBlacklist() {
		LocalDateTime now = LocalDateTime.now();
		tokenBlacklistRepository.deleteAllExpiredBefore(now);
		log.info("[SCHEDULER] 만료된 blacklist 행 정리 완료. 기준시각={}", now);
	}
}
