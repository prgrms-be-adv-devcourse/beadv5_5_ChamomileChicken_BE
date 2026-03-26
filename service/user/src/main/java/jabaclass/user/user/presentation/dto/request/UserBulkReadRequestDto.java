package jabaclass.user.user.presentation.dto.request;

import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UserBulkReadRequestDto(
	@NotNull(message = "사용자 ID 목록은 null일 수 없습니다.")
	@Size(max = 200, message = "한 번에 최대 200개의 사용자 ID를 조회할 수 있습니다.")
	List<@NotNull(message = "사용자 ID는 null일 수 없습니다.") UUID> userIds
) {
}