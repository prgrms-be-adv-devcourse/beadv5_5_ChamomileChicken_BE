package jabaclass.admin.settlement.presentation.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jabaclass.admin.common.dto.ApiResponseDto;
import jabaclass.admin.settlement.application.usecase.SettlementAdminUseCase;
import jabaclass.admin.settlement.presentation.dto.response.SettlementAdminResponseDto;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/admins/settlements")
@RequiredArgsConstructor
public class SettlementAdminController implements SettlementAdminApi {

	private final SettlementAdminUseCase settlementAdminUseCase;

	@Override
	@GetMapping
	public ResponseEntity<ApiResponseDto<Page<SettlementAdminResponseDto>>> getSettlements(Pageable pageable) {
		return ResponseEntity.ok(
			ApiResponseDto.success(HttpStatus.OK, "정산 현황 조회 성공", settlementAdminUseCase.getSettlements(pageable))
		);
	}
}
