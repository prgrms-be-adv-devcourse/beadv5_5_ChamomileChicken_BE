package jabaclass.settlement.presentation.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jabaclass.settlement.application.exception.CommonErrorCode;
import jabaclass.settlement.application.exception.SettlementErrorCode;
import jabaclass.settlement.presentation.apidocs.ApiErrorSpec;
import jabaclass.settlement.presentation.apidocs.ApiErrorSpecs;
import jabaclass.settlement.presentation.dto.response.SettlementResponse;

@Tag(name = "Settlement", description = "정산 조회 API")
public interface SettlementApi {

	@Operation(
		summary = "월별 정산 목록 조회",
		description = """
			특정 월의 정산 목록을 조회합니다.
			- month 형식은 yyyy-MM 입니다.
			- 해당 월에 생성된 모든 정산을 반환합니다.
			"""
	)
	@ApiResponse(
		responseCode = "200",
		description = "정산 목록 조회 성공",
		content = @Content(array = @ArraySchema(schema = @Schema(implementation = SettlementResponse.class)))
	)
	@ApiErrorSpecs({
		@ApiErrorSpec(
			value = CommonErrorCode.class,
			constant = "INVALID_PARAMETER",
			summary = "요청 파라미터 형식이 올바르지 않습니다"
		)
	})
	ResponseEntity<List<SettlementResponse>> getSettlementsByMonth(
		@Parameter(description = "조회할 정산 월", example = "2026-03")
		@RequestParam String month
	);

	@Operation(
		summary = "월별 READY 정산 목록 조회",
		description = """
			특정 월의 READY 상태 정산 목록만 조회합니다.
			- 송금 대기 중인 정산 확인용입니다.
			"""
	)
	@ApiResponse(
		responseCode = "200",
		description = "READY 정산 목록 조회 성공",
		content = @Content(array = @ArraySchema(schema = @Schema(implementation = SettlementResponse.class)))
	)
	@ApiErrorSpecs({
		@ApiErrorSpec(
			value = CommonErrorCode.class,
			constant = "INVALID_PARAMETER",
			summary = "요청 파라미터 형식이 올바르지 않습니다"
		)
	})
	ResponseEntity<List<SettlementResponse>> getReadySettlementsByMonth(
		@Parameter(description = "조회할 정산 월", example = "2026-03")
		@RequestParam String month
	);

	@Operation(
		summary = "정산 단건 조회",
		description = """
			정산 ID로 단건 정산 정보를 조회합니다.
			- 존재하지 않는 정산 ID면 예외가 발생합니다.
			"""
	)
	@ApiResponse(
		responseCode = "200",
		description = "정산 단건 조회 성공",
		content = @Content(schema = @Schema(implementation = SettlementResponse.class))
	)
	@ApiErrorSpecs({
		@ApiErrorSpec(
			value = SettlementErrorCode.class,
			constant = "SETTLEMENT_NOT_FOUND",
			summary = "정산 정보를 찾을 수 없습니다"
		)
	})
	ResponseEntity<SettlementResponse> getSettlement(
		@Parameter(description = "정산 ID")
		@PathVariable UUID settlementId
	);
}
