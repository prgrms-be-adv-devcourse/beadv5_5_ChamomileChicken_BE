package jabaclass.settlement.infrastructure.batch.scheduler;

import java.time.YearMonth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import jabaclass.settlement.application.exception.BusinessException;
import jabaclass.settlement.infrastructure.batch.launcher.SettlementBatchJobLauncher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@Slf4j
@Component
public class SettlementScheduler {

	private final SettlementBatchJobLauncher settlementBatchJobLauncher;

	@Value("${settlement.batch.calculate.target-month-offset-months:1}")
	private int calculateTargetMonthOffsetMonths;

	@Value("${settlement.batch.transfer.target-month-offset-months:1}")
	private int transferTargetMonthOffsetMonths;

	/**
	 * 매월 1일 새벽 2시
	 * 지난달 정산 계산
	 */
	@Scheduled(cron = "${settlement.batch.calculate.cron:0 0 2 1 * *}")
	public void runSettlementCalculateJob() {
		YearMonth settlementMonth = YearMonth.now().minusMonths(calculateTargetMonthOffsetMonths);

		try {
			settlementBatchJobLauncher.startCalculate(settlementMonth);
			log.info("정산 계산 Job 실행 요청 완료. settlementMonth={}", settlementMonth);
		} catch (BusinessException e) {
			log.warn("정산 계산 Job 실행 요청 실패. settlementMonth={} message={}", settlementMonth, e.getMessage());
		} catch (Exception e) {
			log.error("정산 계산 Job 실행 요청 중 알 수 없는 오류 발생. settlementMonth={}", settlementMonth, e);
		}
	}

	/**
	 * 매월 2일 새벽 2시
	 * 정산 이체 실행
	 */
	@Scheduled(cron = "${settlement.batch.transfer.cron:0 0 2 2 * *}")
	public void runSettlementTransferJob() {
		YearMonth settlementMonth = YearMonth.now().minusMonths(transferTargetMonthOffsetMonths);

		try {
			settlementBatchJobLauncher.startTransfer(settlementMonth);
			log.info("정산 이체 Job 실행 요청 완료. settlementMonth={}", settlementMonth);
		} catch (BusinessException e) {
			log.warn("정산 이체 Job 실행 요청 실패. settlementMonth={} message={}", settlementMonth, e.getMessage());
		} catch (Exception e) {
			log.error("정산 이체 Job 실행 요청 중 알 수 없는 오류 발생. settlementMonth={}", settlementMonth, e);
		}
	}
}
