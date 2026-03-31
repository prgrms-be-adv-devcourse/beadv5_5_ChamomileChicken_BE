package jabaclass.product.domain.model;

import java.util.UUID;

public record ProductImageItem(

        // JSON 매핑용
        UUID fileId,
        String storagePath
) {}
