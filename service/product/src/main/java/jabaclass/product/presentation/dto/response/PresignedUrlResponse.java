package jabaclass.product.presentation.dto.response;

public record PresignedUrlResponse(
        String storagePath,
        String presignedUrl
) {}
