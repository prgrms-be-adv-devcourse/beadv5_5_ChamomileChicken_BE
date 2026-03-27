package jabaclass.product.presentation.dto.request;

import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "주문 API 통신")
public record OrderRequestDto(
	@Schema(description = "주문 일정 Id", example = "550e8400-e29b-41d4-a716-446655440000")
	UUID productScheduleId,

	@Schema(description = "주문자 Id", example = "550e8400-e29b-41d4-a716-446655440001")
	UUID userId,

	@Schema(description = "주문 예약 상태", example = "PENDING")
	ReservationRequestStatus status,

	@Schema(description = "상품 소유자 Id", example = "550e8400-e29b-41d4-a716-446655440002", nullable = true)
	UUID productUserId,

	@Schema(description = "예약 인원", example = "2")
	int quantity
) {
}
