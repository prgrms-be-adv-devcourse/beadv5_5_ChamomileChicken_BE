package jabaclass.product.presentation.dto.respose;

import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;
import jabaclass.product.domain.model.status.ReservedStatus;
import jakarta.validation.constraints.NotNull;

@Schema(description = "상품 삭제 응답")
public record DeleteScheduleResposeDto(
	@NotNull(message = "상품 Id를 입력해주세요.")
	@Schema(description = "상품 일정 ID", example = "11111111-1111-1111-1111-111111111111")
	UUID scheduleId,

	@Schema(description = "상태", example = "AVAILABLE")
	ReservedStatus status
) {

	public static DeleteScheduleResposeDto from(UUID scheduleId, ReservedStatus status) {
		return new DeleteScheduleResposeDto(
			scheduleId, status
		);
	}

}
