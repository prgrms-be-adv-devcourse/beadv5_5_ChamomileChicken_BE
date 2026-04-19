package jabaclass.product.presentation.dto.response;

import java.util.UUID;

public record FileConfirmResponse (
    UUID fileId,
    String storagePath
) {}
