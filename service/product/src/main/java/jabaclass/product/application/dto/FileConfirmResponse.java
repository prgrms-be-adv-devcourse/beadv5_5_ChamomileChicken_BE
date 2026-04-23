package jabaclass.product.application.dto;

import java.util.UUID;

public record FileConfirmResponse (
    UUID fileId,
    String storagePath
) {}
