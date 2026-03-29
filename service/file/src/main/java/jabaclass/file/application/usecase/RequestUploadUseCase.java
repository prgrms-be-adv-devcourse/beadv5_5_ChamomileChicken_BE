package jabaclass.file.application.usecase;

import jabaclass.file.presentation.dto.request.UploadRequestDto;
import jabaclass.file.presentation.dto.response.UploadResponseDto;
import java.util.UUID;

public interface RequestUploadUseCase {
    UploadResponseDto requestUpload(UUID userId, UploadRequestDto requestDto);
}
