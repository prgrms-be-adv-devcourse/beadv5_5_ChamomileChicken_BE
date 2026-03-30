package jabaclass.product.presentation.dto.request;

import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;
import jabaclass.product.domain.model.status.OrderStatus;
import jakarta.validation.constraints.NotNull;

@Schema(description = "주문 API 통신")
public record OrderRequestDto(

	@Schema(description = "주문 일정 Id", example = "550e8400-e29b-41d4-a716-446655440000")
	UUID productScheduleId,

	@Schema(description = "사용자 Id", example = "550e8400-e29b-41d4-a716-446655440000")
	UUID userId,

	@NotNull(message = "주문 상태를 입력해주세요.")
	@Schema(description = "주문 상테", example = "PENDING")
	OrderStatus status,

	@Schema(description = "예약 인원", example = "2")
	int quantity,

	@Schema(description = "예약자 테이블 Id", example = "550e8400-e29b-41d4-a716-446655440000")
	UUID productUserId
) {
}
