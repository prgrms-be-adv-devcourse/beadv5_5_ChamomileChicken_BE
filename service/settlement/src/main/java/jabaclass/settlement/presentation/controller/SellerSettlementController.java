package jabaclass.settlement.presentation.controller;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jabaclass.settlement.application.usecase.SettlementUseCase;
import jabaclass.settlement.common.auth.CurrentUser;
import jabaclass.settlement.presentation.dto.response.SellerSettlementDetailPageResponse;
import jabaclass.settlement.presentation.dto.response.SellerSettlementPageResponse;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/seller/settlements")
@RequiredArgsConstructor
public class SellerSettlementController implements SellerSettlementApi {

	private final SettlementUseCase settlementUseCase;

	@GetMapping
	@Override
	public ResponseEntity<SellerSettlementPageResponse> getMySettlements(
		@CurrentUser UUID sellerId,
		@RequestParam(defaultValue = "0") int page,
		@RequestParam(defaultValue = "20") int size
	) {
		return ResponseEntity.ok(settlementUseCase.getSellerSettlements(sellerId, page, size));
	}

	@GetMapping("/{settlementId}/details")
	@Override
	public ResponseEntity<SellerSettlementDetailPageResponse> getMySettlementDetails(
		@CurrentUser UUID sellerId,
		@PathVariable UUID settlementId,
		@RequestParam(defaultValue = "0") int page,
		@RequestParam(defaultValue = "20") int size
	) {
		return ResponseEntity.ok(settlementUseCase.getSellerSettlementDetails(sellerId, settlementId, page, size));
	}
}
