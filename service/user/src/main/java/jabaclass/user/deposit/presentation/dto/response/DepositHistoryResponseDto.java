package jabaclass.user.deposit.presentation.dto.response;

import java.util.List;

public record DepositHistoryResponseDto(
	List<DepositHistoryItemDto> items
) {
}
