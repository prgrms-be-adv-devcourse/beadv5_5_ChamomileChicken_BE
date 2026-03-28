package jabaclass.file.presentation.controller;

import jabaclass.file.application.usecase.CompleteUploadUseCase;
import jabaclass.file.application.usecase.RequestUploadUseCase;
import jabaclass.file.common.dto.ApiResponseDto;
import jabaclass.file.presentation.dto.request.UploadRequestDto;
import jabaclass.file.presentation.dto.response.UploadResponseDto;
import jabastore.auth.util.SecurityUtil;
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

@RestController
@RequestMapping("/api/v1/files")
@RequiredArgsConstructor
public class FileController {

    private final RequestUploadUseCase requestUploadUseCase;
    private final CompleteUploadUseCase completeUploadUseCase;

    @PostMapping("/upload-request")
    public ResponseEntity<ApiResponseDto<UploadResponseDto>> requestUpload(
            @Valid @RequestBody UploadRequestDto request) {
        UUID userId = SecurityUtil.getCurrentUserId();
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponseDto.success(HttpStatus.OK, "업로드 URL 발급 성공",
                        requestUploadUseCase.requestUpload(userId, request)));
    }

    @PatchMapping("/{fileId}/complete")
    public ResponseEntity<ApiResponseDto<Void>> completeUpload(
            @PathVariable UUID fileId) {
        completeUploadUseCase.completeUpload(fileId);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponseDto.success(HttpStatus.OK, "파일 업로드 확정 성공", null));
    }
}
