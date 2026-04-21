package jabaclass.product.presentation.controller;

import jakarta.validation.Valid;
import java.util.UUID;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jabaclass.product.application.usecase.RequestUploadUseCase;
import jabaclass.product.common.exception.ApiResponseDto;
import jabaclass.product.application.usecase.CompleteUploadUseCase;
import jabaclass.product.common.auth.CurrentUser;
import jabaclass.product.presentation.dto.request.UploadRequestDto;
import jabaclass.product.presentation.dto.response.UploadResponseDto;

@RestController
@RequestMapping("/api/v1/files")
@RequiredArgsConstructor
public class FileController implements FileApi {

    private final RequestUploadUseCase requestUploadUseCase;
    private final CompleteUploadUseCase completeUploadUseCase;

	@PostMapping("/upload-request")
	@Override
    public ResponseEntity<ApiResponseDto<UploadResponseDto>> requestUpload(
		@CurrentUser UUID userId,
		@Valid @RequestBody UploadRequestDto request) {
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponseDto.success(HttpStatus.OK, "업로드 URL 발급 성공",
                        requestUploadUseCase.requestUpload(userId, request)));
    }

	@PatchMapping("/{fileId}/complete")
	@Override
    public ResponseEntity<ApiResponseDto<Void>> completeUpload(
            @PathVariable UUID fileId) {
        completeUploadUseCase.completeUpload(fileId);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponseDto.success(HttpStatus.OK, "파일 업로드 확정 성공", null));
    }
}
