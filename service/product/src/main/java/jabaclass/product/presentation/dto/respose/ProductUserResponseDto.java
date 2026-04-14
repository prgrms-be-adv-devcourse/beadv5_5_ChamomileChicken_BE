package jabaclass.product.presentation.dto.respose;

import java.util.Map;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;
import jabaclass.product.domain.model.ProductUser;

@Schema(description = "예약 사용자 조회")
public record ProductUserResponseDto(
	@Schema(description = "상품 예약자 ID", example = "11111111-1111-1111-1111-111111111111")
	UUID id,

	@Schema(description = "상품 일정 ID", example = "11111111-1111-1111-1111-111111111111")
	UUID productScheduleId,

	@Schema(description = "사용자 이름", example = "홍길동")
	String userName,

	@Schema(description = "예약 인원수", example = "2")
	int guestCount,

	@Schema(description = "예약 상태", example = "구매 대기")
	String statusName
) {
	public static ProductUserResponseDto from(ProductUser user, String name) {
		return new ProductUserResponseDto(
			user.getId(),
			user.getProductScheduleId(),
			name,
			user.getGuestCount(),
			user.getStatus().getStatusName()
		);
	}

	public static ProductUserResponseDto listFrom(ProductUser user, Map<UUID, String> map) {
		return new ProductUserResponseDto(
			user.getId(),
			user.getProductScheduleId(),
			map.getOrDefault(user.getUserId(), "사용자 이름이 지정되지 않았습니다."),
			user.getGuestCount(),
			user.getStatus().getStatusName()
		);
	}
}
