package jabaclass.user.user.presentation.dto.request;

import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.NotNull;

public record UserBulkReadRequestDto(
	@NotNull(message = "사용자 ID 목록은 null일 수 없습니다.")
	List<@NotNull(message = "사용자 ID는 null일 수 없습니다.") UUID> userIds
) {
}