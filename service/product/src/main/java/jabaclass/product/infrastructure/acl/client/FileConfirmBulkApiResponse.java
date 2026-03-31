package jabaclass.product.infrastructure.acl.client;

import java.util.List;

public record FileConfirmBulkApiResponse(
        String status,
        String message,
        List<FileConfirmResponse> data
) {}