package jabaclass.file.presentation.controller;

import jabaclass.file.application.usecase.ValidateFileUseCase;
import jabaclass.file.common.dto.ApiResponseDto;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/internal/files")
@RequiredArgsConstructor
public class InternalFileController {

    private final ValidateFileUseCase validateFileUseCase;

    @PostMapping("/{fileId}/validate")
    public ResponseEntity<ApiResponseDto<Void>> validateFile(
            @PathVariable UUID fileId) {
        validateFileUseCase.validateAndConfirm(fileId);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponseDto.success(HttpStatus.OK, "파일 검증 성공", null));
    }
}
