package jabaclass.user.mail.presentation.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jabaclass.user.mail.application.usecase.EmailVerificationUseCase;
import jabaclass.user.mail.presentation.dto.request.SendVerificationCodeRequestDto;
import jabaclass.user.mail.presentation.dto.request.VerifyEmailCodeRequestDto;
import jabaclass.user.mail.presentation.dto.response.VerifyEmailCodeResponseDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/email/verifications")
@RequiredArgsConstructor
public class EmailVerificationController implements EmailVerificationApi {

	private final EmailVerificationUseCase emailVerificationUseCase;

	@Override
	@PostMapping
	public ResponseEntity<Void> sendVerificationCode(
		@Valid @RequestBody SendVerificationCodeRequestDto request
	) {
		emailVerificationUseCase.sendVerificationCode(request.email());
		return ResponseEntity.noContent().build();
	}

	@Override
	@PostMapping("/confirm")
	public ResponseEntity<VerifyEmailCodeResponseDto> verifyCode(
		@Valid @RequestBody VerifyEmailCodeRequestDto request
	) {
		String verifiedToken = emailVerificationUseCase.verifyCode(request.email(), request.code());
		return ResponseEntity.ok(new VerifyEmailCodeResponseDto(verifiedToken));
	}
}