package jabaclass.product.presentation.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jabaclass.product.domain.model.status.ReservedStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

@Schema(description = "상품 일정 등록")
public record CreateScheduleRequestDto(

	@NotBlank
	@Pattern(regexp = "\\d{4}-\\d{2}-\\d{2}", message = "날짜 형식은 yyyy-MM-dd이어야 합니다.")
	@Schema(description = "상품 날짜", example = "2026-03-26")
	String scheduleDt,

	@NotBlank
	@Pattern(regexp = "\\d{2}:\\d{2}", message = "시간 형식은 HH:mm이어야 합니다.")
	@Schema(description = "클래스 시작 시간", example = "12:00")
	String startTime,

	@NotBlank
	@Pattern(regexp = "\\d{2}:\\d{2}", message = "시간 형식은 HH:mm이어야 합니다.")
	@Schema(description = "클래스 종료 시간", example = "13:00")
	String endTime,

	@Schema(description = "예약 상태", example = "AVAILABLE")
	ReservedStatus status
) {
}
