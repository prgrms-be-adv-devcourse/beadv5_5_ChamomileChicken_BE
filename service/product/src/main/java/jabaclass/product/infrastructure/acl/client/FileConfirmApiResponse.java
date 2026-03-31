package jabaclass.product.infrastructure.acl.client;

public record FileConfirmApiResponse(
        int status,
        String message,
        FileConfirmResponse data
) {}
