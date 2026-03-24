package jabaclass.user.auth.presentation.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jabaclass.user.auth.application.exception.AuthErrorCode;
import jabaclass.user.auth.presentation.dto.request.EmailCheckRequestDto;
import jabaclass.user.auth.presentation.dto.request.SendVerificationCodeRequestDto;
import jabaclass.user.auth.presentation.dto.request.VerifyEmailCodeRequestDto;
import jabaclass.user.auth.presentation.dto.response.EmailCheckResponseDto;
import jabaclass.user.common.apidocs.ApiErrorSpec;
import jabaclass.user.common.apidocs.ApiErrorSpecs;
import jakarta.validation.Valid;

@Tag(name = "Email Verification", description = "이메일 인증 API")
public interface EmailVerificationApi {

	@Operation(
		summary = "이메일 중복 확인",
		description = """
			회원가입에 사용할 이메일의 중복 여부를 확인합니다.
			- 중복이 아니면 available=true 를 반환합니다.
			- 이미 사용 중인 이메일이면 예외가 발생합니다.
			"""
	)
	@ApiErrorSpecs({
		@ApiErrorSpec(
			value = AuthErrorCode.class,
			constant = "EMAIL_ALREADY_EXISTS",
			summary = "이미 사용 중인 이메일입니다"
		)
	})
	ResponseEntity<EmailCheckResponseDto> checkEmailDuplicate(
		@Valid @RequestBody EmailCheckRequestDto request
	);

	@Operation(
		summary = "인증번호 발송",
		description = """
			입력한 이메일로 인증번호를 발송합니다.
			- 성공 시 204 No Content를 반환합니다.
			"""
	)
	@ApiErrorSpecs({
		@ApiErrorSpec(
			value = AuthErrorCode.class,
			constant = "EMAIL_ALREADY_EXISTS",
			summary = "이미 사용 중인 이메일입니다"
		)
	})
	ResponseEntity<Void> sendVerificationCode(
		@Valid @RequestBody SendVerificationCodeRequestDto request
	);

	@Operation(
		summary = "인증번호 검증",
		description = """
			이메일로 발송된 인증번호를 검증합니다.
			- 성공 시 204 No Content를 반환합니다.
			"""
	)
	@ApiErrorSpecs({
		@ApiErrorSpec(
			value = AuthErrorCode.class,
			constant = "EMAIL_VERIFICATION_CODE_NOT_FOUND",
			summary = "인증코드가 존재하지 않습니다"
		),
		@ApiErrorSpec(
			value = AuthErrorCode.class,
			constant = "EMAIL_VERIFICATION_CODE_EXPIRED",
			summary = "인증코드가 만료되었습니다"
		),
		@ApiErrorSpec(
			value = AuthErrorCode.class,
			constant = "EMAIL_VERIFICATION_CODE_MISMATCH",
			summary = "인증코드가 올바르지 않습니다"
		)
	})
	ResponseEntity<Void> verifyCode(
		@Valid @RequestBody VerifyEmailCodeRequestDto request
	);
}