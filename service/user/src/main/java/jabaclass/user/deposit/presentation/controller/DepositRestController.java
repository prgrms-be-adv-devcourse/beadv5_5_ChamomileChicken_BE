package jabaclass.user.deposit.presentation.controller;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jabaclass.user.deposit.application.usecase.UseDepositUseCase;
import jabaclass.user.deposit.application.usecase.ValidateDepositUseCase;
import jabaclass.user.deposit.presentation.dto.request.UseDepositRequestDto;
import jabaclass.user.deposit.presentation.dto.request.ValidateDepositRequestDto;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/deposits")
public class DepositRestController {

	private final ValidateDepositUseCase validateDepositUseCase;
	private final UseDepositUseCase useDepositUseCase;

	@PostMapping("/validate")
	public ResponseEntity<Boolean> validateDeposit(
		@AuthenticationPrincipal UUID userId,
		@RequestBody ValidateDepositRequestDto request
	) {
		boolean available = validateDepositUseCase.validate(userId, request.depositAmount());
		return ResponseEntity.ok(available);
	}

	@PostMapping("/use")
	public ResponseEntity<Void> useDeposit(
		@AuthenticationPrincipal UUID userId,
		@RequestBody UseDepositRequestDto request
	) {

		useDepositUseCase.use(userId, request.amount());
		return ResponseEntity.ok().build();
	}
}
