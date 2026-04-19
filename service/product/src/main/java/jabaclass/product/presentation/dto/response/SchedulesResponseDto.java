package jabaclass.product.presentation.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;
import jabaclass.product.domain.model.Schedule;

@Schema(description = "상품 일정 정보")
public record SchedulesResponseDto(
	@Schema(description = "일정 ID", example = "22222222-2222-2222-2222-222222222222")
	UUID id,

	@Schema(description = "상품 ID", example = "22222222-2222-2222-2222-222222222222")
	UUID productId,

	@Schema(description = "상품 날짜", example = "2026-03-26")
	LocalDate scheduleDt,

	@Schema(description = "클래스 시작 시간", example = "12:00")
	LocalTime startTime,

	@Schema(description = "클래스 종료 시간", example = "13:00")
	LocalTime endTime,

	@Schema(description = "예약 상태", example = "예약 가능")
	String status,

	@Schema(description = "남은 예약 가능 인원", example = "10")
	int capacity,

	@Schema(description = "등록일시", example = "2026-03-04T18:10:00")
	LocalDateTime regDt,

	@Schema(description = "수정일시", example = "2026-03-04T18:12:00")
	LocalDateTime modifyDt
) {
	public static SchedulesResponseDto from(Schedule schedule) {
		return new SchedulesResponseDto(
			schedule.getId(),
			schedule.getProductId(),
			schedule.getScheduleDt(),
			schedule.getStartTime(),
			schedule.getEndTime(),
			schedule.getStatus().getStatusName(),
			schedule.getCapacity(),
			schedule.getRegDt(),
			schedule.getModifyDt()
		);
	}
}
