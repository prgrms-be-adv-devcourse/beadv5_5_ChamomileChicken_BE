package jabaclass.settlement.presentation.controller;

import java.time.LocalDate;

import org.springframework.web.bind.annotation.RequestParam;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jabaclass.settlement.application.exception.SettlementBatchErrorCode;
import jabaclass.settlement.common.apidocs.ApiErrorSpec;
import jabaclass.settlement.common.apidocs.ApiErrorSpecs;

@Tag(name = "Settlement Batch", description = "정산 배치 수동 실행 API")
public interface SettlementBatchApi {

	@Operation(
		summary = "정산 대상 적재 배치 실행",
		description = """
			결제/환불 원천 데이터를 정산 대상 테이블에 적재합니다.
			- targetDate 미입력 시 오늘 날짜 기준으로 실행합니다.
			- 과거 날짜를 넣으면 재수집 모드로 동작합니다.
			"""
	)
	@ApiResponse(responseCode = "200", description = "정산 대상 적재 배치 실행 요청 성공")
	@ApiErrorSpecs({
		@ApiErrorSpec(
			value = SettlementBatchErrorCode.class,
			constant = "SETTLEMENT_BATCH_ALREADY_RUNNING",
			summary = "이미 실행 중인 정산 배치입니다"
		),
		@ApiErrorSpec(
			value = SettlementBatchErrorCode.class,
			constant = "SETTLEMENT_BATCH_PARAMETER_INVALID",
			summary = "배치 실행 파라미터가 올바르지 않습니다"
		),
		@ApiErrorSpec(
			value = SettlementBatchErrorCode.class,
			constant = "SETTLEMENT_TARGET_LOAD_FAILED",
			summary = "정산 대상 적재 배치 실행에 실패했습니다"
		)
	})
	String loadTargets(
		@Parameter(description = "적재 대상 날짜", example = "2026-03-30")
		@RequestParam(required = false) LocalDate targetDate
	);

	@Operation(
		summary = "정산 계산 배치 실행",
		description = """
			월 정산을 계산하고 정산/정산 이력을 생성합니다.
			- settlementMonth 미입력 시 이전 달을 기준으로 실행합니다.
			"""
	)
	@ApiResponse(responseCode = "200", description = "정산 계산 배치 실행 요청 성공")
	@ApiErrorSpecs({
		@ApiErrorSpec(
			value = SettlementBatchErrorCode.class,
			constant = "SETTLEMENT_BATCH_ALREADY_RUNNING",
			summary = "이미 실행 중인 정산 배치입니다"
		),
		@ApiErrorSpec(
			value = SettlementBatchErrorCode.class,
			constant = "SETTLEMENT_BATCH_PARAMETER_INVALID",
			summary = "배치 실행 파라미터가 올바르지 않습니다"
		),
		@ApiErrorSpec(
			value = SettlementBatchErrorCode.class,
			constant = "SETTLEMENT_CALCULATE_FAILED",
			summary = "정산 계산 배치 실행에 실패했습니다"
		)
	})
	String calculate(
		@Parameter(description = "계산할 정산 월", example = "2026-03")
		@RequestParam(required = false) String settlementMonth
	);

	@Operation(
		summary = "정산 송금 배치 실행",
		description = """
			READY 상태 정산을 대상으로 송금을 시도합니다.
			- settlementMonth 미입력 시 이전 달을 기준으로 실행합니다.
			"""
	)
	@ApiResponse(responseCode = "200", description = "정산 송금 배치 실행 요청 성공")
	@ApiErrorSpecs({
		@ApiErrorSpec(
			value = SettlementBatchErrorCode.class,
			constant = "SETTLEMENT_BATCH_ALREADY_RUNNING",
			summary = "이미 실행 중인 정산 배치입니다"
		),
		@ApiErrorSpec(
			value = SettlementBatchErrorCode.class,
			constant = "SETTLEMENT_BATCH_PARAMETER_INVALID",
			summary = "배치 실행 파라미터가 올바르지 않습니다"
		),
		@ApiErrorSpec(
			value = SettlementBatchErrorCode.class,
			constant = "SETTLEMENT_TRANSFER_FAILED",
			summary = "정산 송금 배치 실행에 실패했습니다"
		)
	})
	String transfer(
		@Parameter(description = "송금할 정산 월", example = "2026-03")
		@RequestParam(required = false) String settlementMonth
	);
}
