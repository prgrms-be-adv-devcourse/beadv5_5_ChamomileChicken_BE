package jabaclass.order.presentation.dto.request;

import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.NotEmpty;

public record OrderBulkReadRequestDto(
    @NotEmpty List<UUID> orderIds
) {
}
