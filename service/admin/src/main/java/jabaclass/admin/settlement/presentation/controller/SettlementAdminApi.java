package jabaclass.admin.settlement.presentation.controller;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestParam;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jabaclass.admin.common.dto.ApiResponseDto;
import jabaclass.admin.settlement.presentation.dto.response.SettlementAdminResponseDto;

@Tag(name = "Admin - Settlement", description = "어드민 정산 조회 API")
public interface SettlementAdminApi {

	@Operation(summary = "정산 현황 조회 (필터링/검색)")
	ResponseEntity<ApiResponseDto<Page<SettlementAdminResponseDto>>> getSettlements(
		Pageable pageable,
		@Parameter(description = "상태 필터 (READY/TRANSFERRING/SENT/FAILED/HOLD)") @RequestParam(required = false) String status,
		@Parameter(description = "판매자 ID 필터") @RequestParam(required = false) UUID sellerId,
		@Parameter(description = "정산월 필터 (예: 2024-01)") @RequestParam(required = false) String settlementMonth
	);
}
