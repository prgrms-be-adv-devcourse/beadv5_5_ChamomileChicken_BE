package jabaclass.product.application.usecase;

import jabaclass.product.presentation.dto.request.UploadRequestDto;
import jabaclass.product.presentation.dto.response.UploadResponseDto;
import java.util.UUID;

public interface RequestUploadUseCase {
    UploadResponseDto requestUpload(UUID userId, UploadRequestDto requestDto);
}
