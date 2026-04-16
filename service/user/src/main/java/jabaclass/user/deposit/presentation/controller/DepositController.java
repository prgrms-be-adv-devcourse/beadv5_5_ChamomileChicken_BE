package jabaclass.user.deposit.presentation.controller;

import java.util.UUID;

import jabaclass.user.common.auth.CurrentUser;
import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jabaclass.user.deposit.application.usecase.DepositUseCase;
import jabaclass.user.deposit.presentation.dto.request.IncreaseDepositRequestDto;
import jabaclass.user.deposit.presentation.dto.response.DepositDetailResponseDto;
import jabaclass.user.deposit.presentation.dto.response.DepositHistoryResponseDto;
import jabaclass.user.deposit.presentation.dto.response.DepositMeResponseDto;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/deposits")
public class DepositController implements DepositApi {

	private final DepositUseCase depositUseCase;

	@Override
	@GetMapping
	public ResponseEntity<DepositHistoryResponseDto> findAllDepositHistories(
		@CurrentUser UUID userId
	) {
		return ResponseEntity.ok(depositUseCase.findAllDepositHistories(userId));
	}

	@Override
	@GetMapping("/me")
	public ResponseEntity<DepositMeResponseDto> findMyDeposit(
		@CurrentUser UUID userId
	) {
		return ResponseEntity.ok(depositUseCase.findMyDeposit(userId));
	}

	@Override
	@GetMapping("/{depositHistoryId}")
	public ResponseEntity<DepositDetailResponseDto> findDepositHistory(
		@PathVariable UUID depositHistoryId
	) {
		return ResponseEntity.ok(depositUseCase.findDepositHistory(depositHistoryId));
	}

	@PutMapping("/internal/users/{userId}/deposit")
	public ResponseEntity<Void> increaseDeposit(
		@PathVariable UUID userId,
		@RequestBody IncreaseDepositRequestDto request
	) {
		depositUseCase.increase(userId, request.amount(), request.paymentId());
		return ResponseEntity.ok().build();
	}

	@PutMapping("/internal/users/{userId}/refund")
	public ResponseEntity<Void> refundDeposit(
		@PathVariable UUID userId,
		@RequestBody IncreaseDepositRequestDto request
	) {
		depositUseCase.refund(null, userId, request.amount(), request.paymentId());
		return ResponseEntity.ok().build();
	}
}
