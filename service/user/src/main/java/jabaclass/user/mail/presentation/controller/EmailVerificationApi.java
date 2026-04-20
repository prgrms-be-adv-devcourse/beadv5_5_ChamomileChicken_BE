package jabaclass.user.mail.presentation.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jabaclass.user.mail.application.exception.MailErrorCode;
import jabaclass.user.mail.presentation.dto.request.SendVerificationCodeRequestDto;
import jabaclass.user.mail.presentation.dto.request.VerifyEmailCodeRequestDto;
import jabaclass.user.mail.presentation.dto.response.VerifyEmailCodeResponseDto;
import jabaclass.user.common.apidocs.ApiErrorSpec;
import jabaclass.user.common.apidocs.ApiErrorSpecs;
import jakarta.validation.Valid;

@Tag(name = "Email Verification", description = "이메일 인증 API")
public interface EmailVerificationApi {

	@Operation(
		summary = "이메일 인증번호 발송",
		description = """
            입력한 이메일로 인증번호를 발송합니다.
            - 성공 시 204 No Content를 반환합니다.
            """
	)
	ResponseEntity<Void> sendVerificationCode(
		@Valid @RequestBody SendVerificationCodeRequestDto request
	);

	@Operation(
		summary = "이메일 인증번호 검증",
		description = """
            이메일로 발송된 인증번호를 검증합니다.
            - 성공 시 verifiedToken 을 반환합니다.
            """
	)
	@ApiErrorSpecs({
		@ApiErrorSpec(
			value = MailErrorCode.class,
			constant = "EMAIL_VERIFICATION_NOT_FOUND",
			summary = "이메일 인증 정보가 존재하지 않습니다"
		),
		@ApiErrorSpec(
			value = MailErrorCode.class,
			constant = "EMAIL_VERIFICATION_CODE_EXPIRED",
			summary = "인증코드가 만료되었습니다"
		),
		@ApiErrorSpec(
			value = MailErrorCode.class,
			constant = "EMAIL_VERIFICATION_CODE_MISMATCH",
			summary = "인증코드가 올바르지 않습니다"
		)
	})
	ResponseEntity<VerifyEmailCodeResponseDto> verifyCode(
		@Valid @RequestBody VerifyEmailCodeRequestDto request
	);
}