package jabaclass.product.presentation.dto.request;

import java.math.BigDecimal;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "주문 API 통신")
public record OrderRequestDto(

	@Schema(description = "주문 일정 Id", example = "550e8400-e29b-41d4-a716-446655440000")
	UUID productScheduleId,

	@Schema(description = "사용자 Id", example = "550e8400-e29b-41d4-a716-446655440000")
	UUID userId,

	@Schema(description = "예약 인원", example = "2")
	int quantity,

	@Schema(description = "예약자 테이블 Id", example = "550e8400-e29b-41d4-a716-446655440000")
	UUID productUserId,

	@Schema(description = "가격", example = "50000")
	BigDecimal price
) {
}
