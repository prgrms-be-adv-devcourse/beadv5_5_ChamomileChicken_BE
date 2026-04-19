package jabaclass.product.application.usecase;

import jabaclass.product.presentation.dto.response.FileConfirmResponse;
import java.util.UUID;

public interface ValidateFileUseCase {
    FileConfirmResponse validateAndConfirm(UUID fileId);
}
