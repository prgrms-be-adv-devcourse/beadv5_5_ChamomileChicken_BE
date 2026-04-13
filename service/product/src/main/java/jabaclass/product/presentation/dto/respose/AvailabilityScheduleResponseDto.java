package jabaclass.product.presentation.dto.respose;

import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;
import jabaclass.product.domain.model.Schedule;
import jabaclass.product.domain.model.status.ReservedStatus;

@Schema(description = "스케줄 상태 응답")
public record AvailabilityScheduleResponseDto(

	@Schema(description = "상품 ID", example = "550e8400-e29b-41d4-a716-446655440000")
	UUID scheduleId,

	@Schema(description = "상품 예약 상태", example = "AVAILABLE")
	ReservedStatus reservationStatus,

	@Schema(description = "총 예약 가능 인원수", example = "10")
	int maxCapacity,

	@Schema(description = "총 예약 완료 인원수", example = "5")
	int reservedCount,

	@Schema(description = "남은 인원수", example = "5")
	int remainingCount

) {

	public static AvailabilityScheduleResponseDto from(Schedule schedule, int reservedCount, int remainingCount) {
		return new AvailabilityScheduleResponseDto(
			schedule.getId(),
			schedule.getStatus(),
			schedule.getCapacity(),
			reservedCount,
			remainingCount
		);
	}
}
