package jabaclass.admin.settlement.presentation.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jabaclass.admin.common.dto.ApiResponseDto;
import jabaclass.admin.settlement.presentation.dto.response.SettlementAdminResponseDto;

@Tag(name = "Admin - Settlement", description = "어드민 정산 조회 API")
public interface SettlementAdminApi {

	@Operation(summary = "정산 현황 조회")
	ResponseEntity<ApiResponseDto<Page<SettlementAdminResponseDto>>> getSettlements(Pageable pageable);
}
