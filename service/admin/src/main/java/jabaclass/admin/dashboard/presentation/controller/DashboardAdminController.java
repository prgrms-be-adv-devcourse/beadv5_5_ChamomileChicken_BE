package jabaclass.admin.dashboard.presentation.controller;

import java.time.Year;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jabaclass.admin.common.dto.ApiResponseDto;
import jabaclass.admin.dashboard.application.usecase.DashboardAdminUseCase;
import jabaclass.admin.dashboard.presentation.dto.response.DashboardResponseDto;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/admins/dashboard")
@RequiredArgsConstructor
public class DashboardAdminController implements DashboardAdminApi {

	private final DashboardAdminUseCase dashboardAdminUseCase;

	@Override
	@GetMapping
	public ResponseEntity<ApiResponseDto<DashboardResponseDto>> getDashboard(
		@RequestParam(required = false) Integer year
	) {
		int targetYear = (year != null) ? year : Year.now().getValue();
		DashboardResponseDto response = dashboardAdminUseCase.getDashboard(targetYear);
		return ResponseEntity.ok(ApiResponseDto.success(HttpStatus.OK, "대시보드 조회 성공", response));
	}
}