package jabaclass.user.auth.presentation.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jabaclass.user.auth.application.usecase.EmailVerificationUseCase;
import jabaclass.user.auth.presentation.dto.request.EmailCheckRequestDto;
import jabaclass.user.auth.presentation.dto.request.SendVerificationCodeRequestDto;
import jabaclass.user.auth.presentation.dto.request.VerifyEmailCodeRequestDto;
import jabaclass.user.auth.presentation.dto.response.EmailCheckResponseDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class EmailVerificationController implements EmailVerificationApi {

	private final EmailVerificationUseCase emailVerificationUseCase;

	@Override
	@PostMapping("/email-check")
	public ResponseEntity<EmailCheckResponseDto> checkEmailDuplicate(
		@Valid @RequestBody EmailCheckRequestDto request
	) {
		emailVerificationUseCase.checkEmailDuplicate(request.email());
		return ResponseEntity.ok(new EmailCheckResponseDto(true));
	}

	@Override
	@PostMapping("/email")
	public ResponseEntity<Void> sendVerificationCode(
		@Valid @RequestBody SendVerificationCodeRequestDto request
	) {
		emailVerificationUseCase.sendVerificationCode(request.email());
		return ResponseEntity.noContent().build();
	}

	@Override
	@PostMapping("/email/validation")
	public ResponseEntity<Void> verifyCode(
		@Valid @RequestBody VerifyEmailCodeRequestDto request
	) {
		emailVerificationUseCase.verifyCode(request.email(), request.code());
		return ResponseEntity.noContent().build();
	}
}

//Todo 추후 시큐리티 permitAll() 추가
//public static final String AUTH_EMAIL_CHECK = "/api/v1/auth/email-check";
//public static final String AUTH_EMAIL_SEND = "/api/v1/auth/email";
//public static final String AUTH_EMAIL_VERIFY = "/api/v1/auth/email/validation";