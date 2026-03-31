package jabaclass.user.user.presentation.dto.request;

import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.NotEmpty;

public record SellerBulkReadRequestDto(
	@NotEmpty(message = "sellerIds는 비어 있을 수 없습니다.")
	List<UUID> sellerIds
) {
}
