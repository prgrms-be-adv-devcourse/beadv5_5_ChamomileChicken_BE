package jabaclass.admin.dashboard.presentation.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestParam;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jabaclass.admin.common.dto.ApiResponseDto;
import jabaclass.admin.dashboard.presentation.dto.response.DashboardResponseDto;

@Tag(name = "Admin - Dashboard", description = "어드민 통계 대시보드 API")
public interface DashboardAdminApi {

	@Operation(summary = "통계 대시보드 조회", description = "월별 주문 수/매출액, 도메인별 현황 수치, 신규 가입자 추이를 조회합니다.")
	ResponseEntity<ApiResponseDto<DashboardResponseDto>> getDashboard(
		@RequestParam(required = false) Integer year
	);
}