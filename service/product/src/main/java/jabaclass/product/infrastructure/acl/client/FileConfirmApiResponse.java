package jabaclass.product.infrastructure.acl.client;

public record FileConfirmApiResponse(
        String status,
        String message,
        FileConfirmResponse data
) {}
