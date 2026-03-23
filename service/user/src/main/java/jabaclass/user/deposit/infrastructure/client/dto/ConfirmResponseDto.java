package jabaclass.user.deposit.infrastructure.client.dto;

import java.util.UUID;

public record ConfirmResponseDto(
	UUID paymentId
) {
}
