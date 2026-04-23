package jabaclass.product.application.usecase;

import jabaclass.product.application.dto.FileConfirmResponse;
import java.util.UUID;

public interface ValidateFileUseCase {
    FileConfirmResponse validateAndConfirm(UUID fileId);
}
