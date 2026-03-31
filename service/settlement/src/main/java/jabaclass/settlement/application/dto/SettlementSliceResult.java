package jabaclass.settlement.application.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record SettlementSliceResult<T>(
	List<T> content,
	boolean hasNext,
	LocalDateTime nextCursorUpdatedAt,
	UUID nextCursorId
) {
	public SettlementSliceResult {
		content = content == null ? List.of() : List.copyOf(content);
	}
}