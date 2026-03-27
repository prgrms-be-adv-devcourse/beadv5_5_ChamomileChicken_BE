package jabaclass.product.presentation.dto.request;

import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;
import jabaclass.product.domain.model.status.OrderStatus;

@Schema(description = "예약자 생성")
public record CreateProductUserRequestDto(

	@Schema(description = "스케줄 ID", example = "11111111-1111-1111-1111-111111111111")
	UUID productScheduleId,

	@Schema(description = "예약자 ID", example = "11111111-1111-1111-1111-111111111111")
	UUID userId,

	@Schema(description = "예약 인원수", example = "3")
	int guestCount,

	@Schema(description = "구매 상태", example = "PENDING_PURCHASE")
	OrderStatus status
) {
}
