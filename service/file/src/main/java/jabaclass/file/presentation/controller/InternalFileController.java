package jabaclass.file.presentation.controller;

import jabaclass.file.application.usecase.ValidateFileUseCase;
import jabaclass.file.common.dto.ApiResponseDto;
import jabaclass.file.presentation.dto.response.FileConfirmResponse;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/internal/files")
@RequiredArgsConstructor
public class InternalFileController {

    private final ValidateFileUseCase validateFileUseCase;

    @GetMapping("/{fileId}/confirm")
    public ResponseEntity<ApiResponseDto<FileConfirmResponse>> confirmFile(
            @PathVariable UUID fileId) {
        FileConfirmResponse response = validateFileUseCase.validateAndConfirm(fileId);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponseDto.success(HttpStatus.OK, "파일 검증 성공", response));
    }
}
