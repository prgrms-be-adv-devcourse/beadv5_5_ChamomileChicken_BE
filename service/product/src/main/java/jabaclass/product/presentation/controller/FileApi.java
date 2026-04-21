package jabaclass.product.presentation.controller;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;

import jabaclass.product.common.auth.CurrentUser;
import jabaclass.product.common.exception.ApiResponseDto;
import jabaclass.product.presentation.dto.request.UploadRequestDto;
import jabaclass.product.presentation.dto.response.UploadResponseDto;

@Tag(name = "File", description = "파일 업로드 API")
public interface FileApi {

	@Operation(
		summary = "업로드 URL 발급",
		description = """
			로그인한 사용자의 업로드 요청을 받아 presigned PUT URL을 발급합니다.
			- JWT 인증이 필요합니다.
			- 응답으로 fileId, uploadUrl, storagePath를 반환합니다.
			"""
	)
	@SecurityRequirement(name = "bearerAuth")
	ResponseEntity<ApiResponseDto<UploadResponseDto>> requestUpload(
		@CurrentUser UUID userId,
		@Valid @RequestBody UploadRequestDto request
	);

	@Operation(
		summary = "파일 업로드 확정",
		description = """
			S3 업로드가 끝난 파일을 최종 확정 처리합니다.
			- JWT 인증이 필요합니다.
			- fileId 기준으로 업로드 완료 상태를 반영합니다.
			"""
	)
	@SecurityRequirement(name = "bearerAuth")
	ResponseEntity<ApiResponseDto<Void>> completeUpload(
		UUID fileId
	);
}
