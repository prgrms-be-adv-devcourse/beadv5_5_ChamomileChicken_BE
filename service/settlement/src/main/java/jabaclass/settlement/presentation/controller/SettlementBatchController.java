package jabaclass.settlement.presentation.controller;

import java.time.format.DateTimeParseException;
import java.time.YearMonth;

import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.parameters.InvalidJobParametersException;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.launch.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.launch.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.batch.core.launch.JobRestartException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jabaclass.settlement.application.exception.BusinessException;
import jabaclass.settlement.application.exception.SettlementBatchErrorCode;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/internal-batch/settlements")
@RequiredArgsConstructor
public class SettlementBatchController implements SettlementBatchApi {

	private final JobOperator jobOperator;
	private final Job settlementCalculateJob;
	private final Job settlementTransferJob;

	@PostMapping("/calculate")
	@Override
	public String calculate(
		@RequestParam(required = false) String settlementMonth
	) {
		String actualSettlementMonth = settlementMonth == null
			? YearMonth.now().minusMonths(1).toString()
			: settlementMonth;

		JobParameters jobParameters = new JobParametersBuilder()
			.addString("settlementMonth", actualSettlementMonth)
			.addLong("requestedAt", System.currentTimeMillis())
			.toJobParameters();

		try {
			jobOperator.start(settlementCalculateJob, jobParameters);
		} catch (JobExecutionAlreadyRunningException | JobRestartException | JobInstanceAlreadyCompleteException e) {
			throw new BusinessException(SettlementBatchErrorCode.SETTLEMENT_BATCH_ALREADY_RUNNING);
		} catch (InvalidJobParametersException | IllegalArgumentException | DateTimeParseException e) {
			throw new BusinessException(SettlementBatchErrorCode.SETTLEMENT_BATCH_PARAMETER_INVALID);
		} catch (Exception e) {
			throw new BusinessException(SettlementBatchErrorCode.SETTLEMENT_CALCULATE_FAILED);
		}

		return "정산 계산 배치 실행 요청 완료";
	}

	@PostMapping("/transfer")
	@Override
	public String transfer(
		@RequestParam(required = false) String settlementMonth
	) {
		String actualSettlementMonth = settlementMonth == null
			? YearMonth.now().minusMonths(1).toString()
			: settlementMonth;

		JobParameters jobParameters = new JobParametersBuilder()
			.addString("settlementMonth", actualSettlementMonth)
			.addLong("requestedAt", System.currentTimeMillis())
			.toJobParameters();

		try {
			jobOperator.start(settlementTransferJob, jobParameters);
		} catch (JobExecutionAlreadyRunningException | JobRestartException | JobInstanceAlreadyCompleteException e) {
			throw new BusinessException(SettlementBatchErrorCode.SETTLEMENT_BATCH_ALREADY_RUNNING);
		} catch (InvalidJobParametersException | IllegalArgumentException | DateTimeParseException e) {
			throw new BusinessException(SettlementBatchErrorCode.SETTLEMENT_BATCH_PARAMETER_INVALID);
		} catch (Exception e) {
			throw new BusinessException(SettlementBatchErrorCode.SETTLEMENT_TRANSFER_FAILED);
		}

		return "정산 송금 배치 실행 요청 완료";
	}
}
