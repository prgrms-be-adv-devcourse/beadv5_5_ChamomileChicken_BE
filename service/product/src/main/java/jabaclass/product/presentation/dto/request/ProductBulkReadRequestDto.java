package jabaclass.product.presentation.dto.request;

import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.NotEmpty;

public record ProductBulkReadRequestDto(
	@NotEmpty(message = "productIds는 비어 있을 수 없습니다.")
	List<UUID> productIds
) {
}
