package jabaclass.product.presentation.dto.request;

import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "주문 API 통신")
public record OrderRequestDto(
	@Schema(description = "주문 Id", example = "550e8400-e29b-41d4-a716-446655440000")
	UUID productScheduleId,

	@Schema(description = "예약 인원", example = "2")
	int quantity
) {
}
