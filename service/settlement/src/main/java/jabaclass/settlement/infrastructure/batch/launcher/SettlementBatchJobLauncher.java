package jabaclass.settlement.infrastructure.batch.launcher;

import java.time.YearMonth;
import java.time.format.DateTimeParseException;

import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.parameters.InvalidJobParametersException;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.launch.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.launch.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.batch.core.launch.JobRestartException;
import org.springframework.stereotype.Component;

import jabaclass.settlement.application.exception.BusinessException;
import jabaclass.settlement.application.exception.ErrorCode;
import jabaclass.settlement.application.exception.SettlementBatchErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class SettlementBatchJobLauncher {

	private final JobOperator jobOperator;
	private final Job settlementCalculateJob;
	private final Job settlementTransferJob;

	public void startCalculate(String settlementMonth) {
		startWithMonthParam(
			settlementCalculateJob,
			settlementMonth,
			YearMonth.now().minusMonths(1),
			SettlementBatchErrorCode.SETTLEMENT_CALCULATE_FAILED
		);
	}

	public void startCalculate(YearMonth settlementMonth) {
		start(
			settlementCalculateJob,
			settlementMonth.toString(),
			SettlementBatchErrorCode.SETTLEMENT_CALCULATE_FAILED
		);
	}

	public void startTransfer(String settlementMonth) {
		startWithMonthParam(
			settlementTransferJob,
			settlementMonth,
			YearMonth.now().minusMonths(1),
			SettlementBatchErrorCode.SETTLEMENT_TRANSFER_FAILED
		);
	}

	public void startTransfer(YearMonth settlementMonth) {
		start(
			settlementTransferJob,
			settlementMonth.toString(),
			SettlementBatchErrorCode.SETTLEMENT_TRANSFER_FAILED
		);
	}

	private void startWithMonthParam(
		Job job,
		String settlementMonthParam,
		YearMonth defaultSettlementMonth,
		ErrorCode failureErrorCode
	) {
		try {
			start(job, resolveSettlementMonth(settlementMonthParam, defaultSettlementMonth), failureErrorCode);
		} catch (DateTimeParseException e) {
			throw new BusinessException(SettlementBatchErrorCode.SETTLEMENT_BATCH_PARAMETER_INVALID);
		}
	}

	private void start(Job job, String settlementMonth, ErrorCode failureErrorCode) {
		try {
			JobParameters jobParameters = new JobParametersBuilder()
				.addString("settlementMonth", settlementMonth)
				.addLong("requestedAt", System.currentTimeMillis())
				.toJobParameters();

			jobOperator.start(job, jobParameters);
		} catch (JobExecutionAlreadyRunningException | JobRestartException | JobInstanceAlreadyCompleteException e) {
			throw new BusinessException(SettlementBatchErrorCode.SETTLEMENT_BATCH_ALREADY_RUNNING);
		} catch (InvalidJobParametersException | IllegalArgumentException | DateTimeParseException e) {
			throw new BusinessException(SettlementBatchErrorCode.SETTLEMENT_BATCH_PARAMETER_INVALID);
		} catch (Exception e) {
			log.error("[SETTLEMENT_BATCH] Job 실행 중 알 수 없는 예외 발생. jobName={}, settlementMonth={}",
				job.getName(),
				settlementMonth,
				e
			);
			throw new BusinessException(failureErrorCode);
		}
	}

	private String resolveSettlementMonth(String settlementMonth, YearMonth defaultSettlementMonth) {
		if (settlementMonth == null) {
			return defaultSettlementMonth.toString();
		}

		return YearMonth.parse(settlementMonth).toString();
	}
}
