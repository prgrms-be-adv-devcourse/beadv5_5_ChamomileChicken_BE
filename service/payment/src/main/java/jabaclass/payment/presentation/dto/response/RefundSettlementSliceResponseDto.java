package jabaclass.payment.presentation.dto.response;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record RefundSettlementSliceResponseDto(
	List<RefundSettlementTargetItemResponseDto> content,
	boolean hasNext,
	LocalDateTime nextCursorUpdatedAt,
	UUID nextCursorId
) {
}
