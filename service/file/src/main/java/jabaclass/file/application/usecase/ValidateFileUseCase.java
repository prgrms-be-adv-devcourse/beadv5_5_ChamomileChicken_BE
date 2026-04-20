package jabaclass.file.application.usecase;

import jabaclass.file.presentation.dto.response.FileConfirmResponse;
import java.util.UUID;

public interface ValidateFileUseCase {
    FileConfirmResponse validateAndConfirm(UUID fileId);
}
