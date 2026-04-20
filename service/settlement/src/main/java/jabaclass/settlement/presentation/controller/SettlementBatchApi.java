package jabaclass.settlement.presentation.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jabaclass.settlement.application.exception.SettlementBatchErrorCode;
import jabaclass.settlement.presentation.apidocs.ApiErrorSpec;
import jabaclass.settlement.presentation.apidocs.ApiErrorSpecs;

@Tag(name = "Settlement Batch", description = "정산 배치 수동 실행 API")
public interface SettlementBatchApi {

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
	String calculate(String settlementMonth);

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
	String transfer(String settlementMonth);
}
