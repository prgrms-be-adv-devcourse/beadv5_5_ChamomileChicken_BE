package jabaclass.settlement.presentation.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jabaclass.settlement.application.usecase.SettlementUseCase;
import jabaclass.settlement.domain.model.settlement.Settlement;
import jabaclass.settlement.presentation.dto.response.SettlementResponse;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/settlements")
@RequiredArgsConstructor
public class SettlementController implements SettlementApi {

	private final SettlementUseCase settlementUseCase;

	/**
	 * 특정 월 정산 목록 조회
	 * 예: /settlements?month=2026-03
	 */
	@GetMapping
	@Override
	public ResponseEntity<List<SettlementResponse>> getSettlementsByMonth(
		@RequestParam String month
	) {
		List<SettlementResponse> settlements = settlementUseCase.getSettlementsByMonth(month).stream()
			.map(SettlementResponse::from)
			.toList();

		return ResponseEntity.ok(settlements);
	}

	/**
	 * 특정 월 READY 상태 정산 목록 조회
	 * 예: /settlements/ready?month=2026-03
	 */
	@GetMapping("/ready")
	@Override
	public ResponseEntity<List<SettlementResponse>> getReadySettlementsByMonth(
		@RequestParam String month
	) {
		List<SettlementResponse> settlements = settlementUseCase.getReadySettlementsByMonth(month).stream()
			.map(SettlementResponse::from)
			.toList();

		return ResponseEntity.ok(settlements);
	}

	/**
	 * 정산 단건 조회
	 * 예: /settlements/{settlementId}
	 */
	@GetMapping("/{settlementId}")
	@Override
	public ResponseEntity<SettlementResponse> getSettlement(
		@PathVariable UUID settlementId
	) {
		Settlement settlement = settlementUseCase.getSettlement(settlementId);
		return ResponseEntity.ok(SettlementResponse.from(settlement));
	}
}
