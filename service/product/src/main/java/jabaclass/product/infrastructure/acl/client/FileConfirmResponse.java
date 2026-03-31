package jabaclass.product.infrastructure.acl.client;

import java.util.UUID;

public record FileConfirmResponse(
        UUID fileId,
        String storagePath
) {}
