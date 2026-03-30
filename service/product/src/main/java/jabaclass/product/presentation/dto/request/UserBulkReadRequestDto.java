package jabaclass.product.presentation.dto.request;

import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.NotNull;

public record UserBulkReadRequestDto(

	@NotNull(message = "판매자 Id를 입력해주세요.")
	List<UUID> userIds
) {
}
