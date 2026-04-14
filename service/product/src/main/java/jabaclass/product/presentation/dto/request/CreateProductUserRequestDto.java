package jabaclass.product.presentation.dto.request;

import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;
import jabaclass.product.domain.model.status.ReservationStatus;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

@Schema(description = "예약자 생성")
public record CreateProductUserRequestDto(

	@NotNull(message = "스케줄 Id를 입력해주세요.")
	@Schema(description = "스케줄 ID", example = "11111111-1111-1111-1111-111111111111")
	UUID productScheduleId,

	@NotNull(message = "예약자 Id를 입력해주세요.")
	@Schema(description = "예약자 ID", example = "11111111-1111-1111-1111-111111111111")
	UUID userId,

	@Min(value = 1, message = "예약 인원수를 입력해주세요.")
	@Schema(description = "예약 인원수", example = "3")
	int guestCount,

	@Schema(description = "구매 상태", example = "PENDING_PURCHASE")
	ReservationStatus status
) {
}
