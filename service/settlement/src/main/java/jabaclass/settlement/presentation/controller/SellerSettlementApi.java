package jabaclass.settlement.presentation.controller;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jabaclass.settlement.application.exception.CommonErrorCode;
import jabaclass.settlement.application.exception.SettlementErrorCode;
import jabaclass.settlement.common.auth.CurrentUser;
import jabaclass.settlement.presentation.apidocs.ApiErrorSpec;
import jabaclass.settlement.presentation.apidocs.ApiErrorSpecs;
import jabaclass.settlement.presentation.dto.response.SellerSettlementDetailPageResponse;
import jabaclass.settlement.presentation.dto.response.SellerSettlementPageResponse;

@Tag(name = "Seller Settlement", description = "판매자 정산 조회 API")
public interface SellerSettlementApi {

	@Operation(
		summary = "판매자 정산 목록 페이지 조회",
		description = """
			헤더의 판매자 ID를 기준으로 자신의 정산 목록을 페이지 조회합니다.
			- 판매자 ID는 X-User-Id 헤더에서 읽습니다.
			"""
	)
	@ApiResponse(
		responseCode = "200",
		description = "판매자 정산 목록 조회 성공",
		content = @Content(schema = @Schema(implementation = SellerSettlementPageResponse.class))
	)
	@ApiErrorSpecs({
		@ApiErrorSpec(
			value = CommonErrorCode.class,
			constant = "INVALID_PARAMETER",
			summary = "요청 파라미터 형식이 올바르지 않습니다"
		)
	})
	ResponseEntity<SellerSettlementPageResponse> getMySettlements(
		@Parameter(hidden = true)
		@CurrentUser UUID sellerId,
		@Parameter(description = "페이지 번호", example = "0")
		@RequestParam(defaultValue = "0") int page,
		@Parameter(description = "페이지 크기", example = "20")
		@RequestParam(defaultValue = "20") int size
	);

	@Operation(
		summary = "판매자 정산 상세 항목 페이지 조회",
		description = """
			헤더의 판매자 ID를 기준으로 자신의 정산 상세 항목을 페이지 조회합니다.
			- 정산 상세 항목은 SettlementTargetCalculation + SettlementTarget 조합으로 구성됩니다.
			"""
	)
	@ApiResponse(
		responseCode = "200",
		description = "판매자 정산 상세 항목 조회 성공",
		content = @Content(schema = @Schema(implementation = SellerSettlementDetailPageResponse.class))
	)
	@ApiErrorSpecs({
		@ApiErrorSpec(
			value = CommonErrorCode.class,
			constant = "INVALID_PARAMETER",
			summary = "요청 파라미터 형식이 올바르지 않습니다"
		),
		@ApiErrorSpec(
			value = SettlementErrorCode.class,
			constant = "SETTLEMENT_NOT_FOUND",
			summary = "정산 정보를 찾을 수 없거나 접근 권한이 없습니다"
		)
	})
	ResponseEntity<SellerSettlementDetailPageResponse> getMySettlementDetails(
		@Parameter(hidden = true)
		@CurrentUser UUID sellerId,
		@Parameter(description = "정산 ID")
		@PathVariable UUID settlementId,
		@Parameter(description = "페이지 번호", example = "0")
		@RequestParam(defaultValue = "0") int page,
		@Parameter(description = "페이지 크기", example = "20")
		@RequestParam(defaultValue = "20") int size
	);
}
