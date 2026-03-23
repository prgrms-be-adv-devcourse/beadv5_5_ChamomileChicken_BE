package jabaclass.user.deposit.presentation.controller;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jabaclass.user.deposit.application.usecase.DepositChargeUseCase;
import jabaclass.user.deposit.application.usecase.DepositQueryUseCase;
import jabaclass.user.deposit.presentation.dto.request.DepositChargeRequestDto;
import jabaclass.user.deposit.presentation.dto.response.DepositChargeResponseDto;
import jabaclass.user.deposit.presentation.dto.response.DepositDetailResponseDto;
import jabaclass.user.deposit.presentation.dto.response.DepositHistoryResponseDto;
import jabaclass.user.deposit.presentation.dto.response.DepositMeResponseDto;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/deposits")
public class DepositController implements DepositApi {

	private final DepositChargeUseCase depositChargeUseCase;
	private final DepositQueryUseCase depositQueryUseCase;

	@Override
	@GetMapping
	public ResponseEntity<DepositHistoryResponseDto> findAllDepositHistories(
		@AuthenticationPrincipal UUID userId
	) {
		return ResponseEntity.ok(depositQueryUseCase.findAllDepositHistories(userId));
	}

	@Override
	@GetMapping("/me")
	public ResponseEntity<DepositMeResponseDto> findMyDeposit(
		@AuthenticationPrincipal UUID userId
	) {
		return ResponseEntity.ok(depositQueryUseCase.findMyDeposit(userId));
	}

	@Override
	@GetMapping("/{depositHistoryId}")
	public ResponseEntity<DepositDetailResponseDto> findDepositHistory(
		@PathVariable UUID depositHistoryId
	) {
		return ResponseEntity.ok(depositQueryUseCase.findDepositHistory(depositHistoryId));
	}

	@Override
	@PostMapping
	public ResponseEntity<DepositChargeResponseDto> chargeDeposit(
		@AuthenticationPrincipal UUID userId,
		@RequestBody DepositChargeRequestDto request
	) {
		return ResponseEntity.ok(depositChargeUseCase.charge(userId, request));
	}
}