package jabaclass.product.presentation.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jabaclass.product.domain.model.status.ReservedStatus;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

@Schema(description = "상품 일정 수정")
public record UpdateSchedulesRequestDto(
	@NotBlank
	@Pattern(regexp = "\\d{2}:\\d{2}", message = "시간 형식은 HH:mm이어야 합니다.")
	@Schema(description = "클래스 시작 시간", example = "12:00")
	String startTime,

	@NotBlank
	@Pattern(regexp = "\\d{2}:\\d{2}", message = "시간 형식은 HH:mm이어야 합니다.")
	@Schema(description = "클래스 종료 시간", example = "13:00")
	String endTime,

	@Schema(description = "예약 상태", example = "AVAILABLE")
	ReservedStatus status,

	@Min(value = 1, message = "예약 가능 인원수를 입력해주세요.")
	@Schema(description = "예약 가능 인원", example = "10")
	int maxCapacity
) {
}
