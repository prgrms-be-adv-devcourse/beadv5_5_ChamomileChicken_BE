package jabaclass.user.user.presentation.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jabaclass.user.common.apidocs.ApiErrorSpec;
import jabaclass.user.common.apidocs.ApiErrorSpecs;
import jabaclass.user.mail.application.exception.MailErrorCode;
import jabaclass.user.user.application.exception.UserErrorCode;
import jabaclass.user.user.presentation.dto.request.ChangeMyEmailRequestDto;
import jabaclass.user.user.presentation.dto.request.EmailCheckRequestDto;
import jabaclass.user.user.presentation.dto.request.RegisterUserRequestDto;
import jabaclass.user.user.presentation.dto.request.UpdateUserRequestDto;
import jabaclass.user.user.presentation.dto.request.UserBulkReadRequestDto;
import jabaclass.user.user.presentation.dto.response.EmailCheckResponseDto;
import jabaclass.user.user.presentation.dto.response.UserResponseDto;
import jakarta.validation.Valid;

@Tag(name = "User", description = "사용자 API")
public interface UserApi {

	@Operation(
		summary = "이메일 중복 확인",
		description = """
            회원가입 또는 이메일 변경에 사용할 이메일의 중복 여부를 확인합니다.
            - 중복이 아니면 available=true 를 반환합니다.
            - 이미 사용 중인 이메일이면 예외가 발생합니다.
            """
	)
	@ApiErrorSpecs({
		@ApiErrorSpec(
			value = UserErrorCode.class,
			constant = "EMAIL_ALREADY_EXISTS",
			summary = "이미 사용 중인 이메일입니다"
		)
	})
	ResponseEntity<EmailCheckResponseDto> checkEmailDuplicate(
		@Valid @RequestBody EmailCheckRequestDto request
	);

	@Operation(
		summary = "회원가입",
		description = """
            이메일 인증이 완료된 사용자가 회원가입을 진행합니다.
            - email, verifiedToken 조합이 유효해야 회원가입할 수 있습니다.
            - 성공 시 201 Created 를 반환합니다.
            """
	)
	@ApiErrorSpecs({
		@ApiErrorSpec(
			value = UserErrorCode.class,
			constant = "EMAIL_ALREADY_EXISTS",
			summary = "이미 사용 중인 이메일입니다"
		),
		@ApiErrorSpec(
			value = MailErrorCode.class,
			constant = "EMAIL_VERIFICATION_TOKEN_NOT_FOUND",
			summary = "이메일 인증 토큰이 존재하지 않습니다"
		),
		@ApiErrorSpec(
			value = MailErrorCode.class,
			constant = "EMAIL_VERIFICATION_TOKEN_EXPIRED",
			summary = "이메일 인증 토큰이 만료되었습니다"
		),
		@ApiErrorSpec(
			value = MailErrorCode.class,
			constant = "EMAIL_VERIFICATION_TOKEN_MISMATCH",
			summary = "이메일 인증 토큰이 올바르지 않습니다"
		)
	})
	ResponseEntity<Void> register(
		@Valid @RequestBody RegisterUserRequestDto request
	);

	@Operation(
		summary = "내 정보 조회",
		description = """
            현재 로그인한 사용자의 정보를 조회합니다.
            """
	)
	@SecurityRequirement(name = "bearerAuth")
	@ApiErrorSpecs({
		@ApiErrorSpec(
			value = UserErrorCode.class,
			constant = "USER_NOT_FOUND",
			summary = "사용자를 찾을 수 없습니다"
		)
	})
	ResponseEntity<UserResponseDto> getMyInfo();

	@Operation(
		summary = "내 정보 수정",
		description = """
            현재 로그인한 사용자의 이름과 전화번호를 수정합니다.
            - 성공 시 204 No Content 를 반환합니다.
            """
	)
	@SecurityRequirement(name = "bearerAuth")
	@ApiErrorSpecs({
		@ApiErrorSpec(
			value = UserErrorCode.class,
			constant = "USER_NOT_FOUND",
			summary = "사용자를 찾을 수 없습니다"
		)
	})
	ResponseEntity<Void> updateMyInfo(
		@Valid @RequestBody UpdateUserRequestDto request
	);

	@Operation(
		summary = "이메일 수정",
		description = """
            현재 로그인한 사용자의 이메일을 수정합니다.
            - 사전에 이메일 인증을 완료하여 verifiedToken 을 발급받아야 합니다.
            - 성공 시 204 No Content 를 반환합니다.
            """
	)
	@SecurityRequirement(name = "bearerAuth")
	@ApiErrorSpecs({
		@ApiErrorSpec(
			value = UserErrorCode.class,
			constant = "USER_NOT_FOUND",
			summary = "사용자를 찾을 수 없습니다"
		),
		@ApiErrorSpec(
			value = UserErrorCode.class,
			constant = "EMAIL_ALREADY_EXISTS",
			summary = "이미 사용 중인 이메일입니다"
		),
		@ApiErrorSpec(
			value = MailErrorCode.class,
			constant = "EMAIL_VERIFICATION_TOKEN_NOT_FOUND",
			summary = "이메일 인증 토큰이 존재하지 않습니다"
		),
		@ApiErrorSpec(
			value = MailErrorCode.class,
			constant = "EMAIL_VERIFICATION_TOKEN_EXPIRED",
			summary = "이메일 인증 토큰이 만료되었습니다"
		),
		@ApiErrorSpec(
			value = MailErrorCode.class,
			constant = "EMAIL_VERIFICATION_TOKEN_MISMATCH",
			summary = "이메일 인증 토큰이 올바르지 않습니다"
		)
	})
	ResponseEntity<Void> changeEmail(
		@Valid @RequestBody ChangeMyEmailRequestDto request
	);

	@Operation(
		summary = "회원 탈퇴",
		description = """
            현재 로그인한 사용자를 탈퇴 처리합니다.
            - 성공 시 204 No Content 를 반환합니다.
            """
	)
	@SecurityRequirement(name = "bearerAuth")
	@ApiErrorSpecs({
		@ApiErrorSpec(
			value = UserErrorCode.class,
			constant = "USER_NOT_FOUND",
			summary = "사용자를 찾을 수 없습니다"
		)
	})
	ResponseEntity<Void> withdraw();

	@Operation(
		summary = "사용자 ID 목록으로 사용자 정보 조회",
		description = """
        사용자 ID 목록을 받아 여러 사용자의 정보를 조회합니다.
        - 요청한 사용자 ID 순서를 기준으로 응답합니다.
        - 존재하지 않는 사용자 ID는 응답에서 제외합니다.
        """
	)
	ResponseEntity<List<UserResponseDto>> getUsersByIds(
		@Valid @RequestBody UserBulkReadRequestDto request
	);
}