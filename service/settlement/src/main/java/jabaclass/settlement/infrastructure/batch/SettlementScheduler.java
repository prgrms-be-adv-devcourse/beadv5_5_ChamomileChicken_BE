package jabaclass.settlement.infrastructure.batch;

import java.time.LocalDate;
import java.time.YearMonth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.job.parameters.InvalidJobParametersException;
import org.springframework.batch.core.launch.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.launch.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.batch.core.launch.JobRestartException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;


@RequiredArgsConstructor
@Slf4j
@Component
public class SettlementScheduler {

	private final JobOperator jobOperator;
	private final Job settlementTargetLoadJob;
	private final Job settlementCalculateJob;
	private final Job settlementTransferJob;

	@Value("${settlement.batch.load.target-date-offset-days:0}")
	private int loadTargetDateOffsetDays;

	@Value("${settlement.batch.calculate.target-month-offset-months:1}")
	private int calculateTargetMonthOffsetMonths;

	@Value("${settlement.batch.transfer.target-month-offset-months:1}")
	private int transferTargetMonthOffsetMonths;

	/**
	 * 매일 새벽 1시
	 * 전날 결제/환불 데이터 기준으로 정산 대상 적재
	 */
	@Scheduled(cron = "${settlement.batch.load.cron:0 0 1 * * *}")
	public void runSettlementTargetLoadJob() {
		LocalDate targetDate = LocalDate.now().plusDays(loadTargetDateOffsetDays);

		try {
			JobParameters jobParameters = new JobParametersBuilder()
				.addString("targetDate", targetDate.toString())
				.addLong("requestedAt", System.currentTimeMillis())
				.toJobParameters();

			jobOperator.start(settlementTargetLoadJob, jobParameters);

			log.info("정산 대상 적재 Job 실행 완료. targetDate={}", targetDate);
		} catch (JobExecutionAlreadyRunningException | JobRestartException | JobInstanceAlreadyCompleteException e) {
			log.warn("정산 대상 적재 Job 중복 실행 요청으로 건너뜀. targetDate={}", targetDate);
		} catch (InvalidJobParametersException | IllegalArgumentException e) {
			log.error("정산 대상 적재 Job 파라미터 오류. targetDate={}", targetDate, e);
		} catch (Exception e) {
			log.error("정산 대상 적재 Job 실행 실패", e);
		}
	}

	/**
	 * 매월 1일 새벽 2시
	 * 지난달 정산 계산
	 */
	@Scheduled(cron = "${settlement.batch.calculate.cron:0 0 2 1 * *}")
	public void runSettlementCalculateJob() {
		YearMonth settlementMonth = YearMonth.now().minusMonths(calculateTargetMonthOffsetMonths);

		try {
			JobParameters jobParameters = new JobParametersBuilder()
				.addString("settlementMonth", settlementMonth.toString())
				.addLong("requestedAt", System.currentTimeMillis())
				.toJobParameters();

			jobOperator.start(settlementCalculateJob, jobParameters);

			log.info("정산 계산 Job 실행 완료. settlementMonth={}", settlementMonth);
		} catch (JobExecutionAlreadyRunningException | JobRestartException | JobInstanceAlreadyCompleteException e) {
			log.warn("정산 계산 Job 중복 실행 요청으로 건너뜀. settlementMonth={}", settlementMonth);
		} catch (InvalidJobParametersException | IllegalArgumentException e) {
			log.error("정산 계산 Job 파라미터 오류. settlementMonth={}", settlementMonth, e);
		} catch (Exception e) {
			log.error("정산 계산 Job 실행 실패", e);
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
			JobParameters jobParameters = new JobParametersBuilder()
				.addString("settlementMonth", settlementMonth.toString())
				.addLong("requestedAt", System.currentTimeMillis())
				.toJobParameters();

			jobOperator.start(settlementTransferJob, jobParameters);

			log.info("정산 이체 Job 실행 완료. settlementMonth={}", settlementMonth);
		} catch (JobExecutionAlreadyRunningException | JobRestartException | JobInstanceAlreadyCompleteException e) {
			log.warn("정산 이체 Job 중복 실행 요청으로 건너뜀. settlementMonth={}", settlementMonth);
		} catch (InvalidJobParametersException | IllegalArgumentException e) {
			log.error("정산 이체 Job 파라미터 오류. settlementMonth={}", settlementMonth, e);
		} catch (Exception e) {
			log.error("정산 이체 Job 실행 실패", e);
		}
	}
}
