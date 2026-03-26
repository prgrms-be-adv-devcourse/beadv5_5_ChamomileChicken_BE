package jabaclass.user.deposit.presentation.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jabaclass.user.deposit.application.usecase.UseDepositUseCase;
import jabaclass.user.deposit.application.usecase.ValidateDepositUseCase;
import jabaclass.user.deposit.presentation.dto.request.UseDepositRequestDto;
import jabaclass.user.deposit.presentation.dto.request.ValidateDepositRequestDto;
import jabaclass.user.deposit.presentation.dto.response.ValidateDepositResponseDto;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/deposits")
public class DepositRestController {

	private final ValidateDepositUseCase validateDepositUseCase;
	private final UseDepositUseCase useDepositUseCase;

	@PostMapping("/validate")
	public ResponseEntity<ValidateDepositResponseDto> validateDeposit(
		@RequestBody ValidateDepositRequestDto request
	) {
		boolean valid = validateDepositUseCase.validate(request.userId(), request.depositAmount());
		return ResponseEntity.ok(new ValidateDepositResponseDto(valid));
	}

	@PostMapping("/use")
	public ResponseEntity<Void> useDeposit(
		@RequestBody UseDepositRequestDto request
	) {

		useDepositUseCase.use(request.userId(), request.depositAmount());
		return ResponseEntity.ok().build();
	}
}
