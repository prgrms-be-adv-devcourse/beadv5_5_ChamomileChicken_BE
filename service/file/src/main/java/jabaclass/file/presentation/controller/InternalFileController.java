package jabaclass.file.presentation.controller;

import jabaclass.file.application.usecase.ValidateFileUseCase;
import jabaclass.file.common.dto.ApiResponseDto;
import jabaclass.file.infrastructure.s3.S3Uploader;
import jabaclass.file.presentation.dto.response.FileConfirmResponse;
import jabaclass.file.presentation.dto.response.PresignedUrlResponse;
import io.swagger.v3.oas.annotations.Hidden;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/internal/files")
@RequiredArgsConstructor
@Hidden
public class InternalFileController {

    private final ValidateFileUseCase validateFileUseCase;
    private final S3Uploader s3Uploader;

    // 단건 — product 단일 이미지 검증용
    @GetMapping("/{fileId}/confirm")
    public ResponseEntity<ApiResponseDto<FileConfirmResponse>> confirmFile(
            @PathVariable UUID fileId) {
        FileConfirmResponse response = validateFileUseCase.validateAndConfirm(fileId);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponseDto.success(HttpStatus.OK, "파일 검증 성공", response));
    }

    // 벌크 — 여러 이미지 한번에 검증용
    @PostMapping("/confirm/bulk")
    public ResponseEntity<ApiResponseDto<List<FileConfirmResponse>>> confirmFiles(
            @RequestBody List<UUID> fileIds) {
        List<FileConfirmResponse> responses = fileIds.stream()
                .map(validateFileUseCase::validateAndConfirm)
                .toList();
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponseDto.success(HttpStatus.OK, "파일 검증 성공", responses));
    }

    // storagePath 목록으로 presigned GET URL 일괄 발급
    @PostMapping("/presigned-urls")
    public ResponseEntity<ApiResponseDto<List<PresignedUrlResponse>>> getPresignedUrls(
            @RequestBody List<String> storagePaths) {
        List<PresignedUrlResponse> responses = storagePaths.stream()
                .map(path -> new PresignedUrlResponse(path, s3Uploader.generatePresignedGetUrl(path)))
                .toList();
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponseDto.success(HttpStatus.OK, "Presigned URL 발급 성공", responses));
    }
}
