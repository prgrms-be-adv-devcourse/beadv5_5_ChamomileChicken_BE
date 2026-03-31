package jabaclass.file.presentation.dto.response;

public record PresignedUrlResponse(
        String storagePath,
        String presignedUrl
) {}