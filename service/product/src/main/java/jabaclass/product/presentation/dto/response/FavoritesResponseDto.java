package jabaclass.product.presentation.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;
import jabaclass.product.domain.model.Favorite;
import jabaclass.product.domain.model.Product;
import jabaclass.product.domain.model.Schedule;

@Schema(description = "즐겨 찾기")
public record FavoritesResponseDto(

	@Schema(description = "즐겨찾기 Id", example = "550e8400-e29b-41d4-a716-446655440000")
	UUID id,

	@Schema(description = "상품 일정 Id", example = "550e8400-e29b-41d4-a716-446655440000")
	UUID productScheduleId,

	@Schema(description = "수량", example = "2")
	int quantity,

	@Schema(description = "상품 Id", example = "550e8400-e29b-41d4-a716-446655440000")
	UUID productId,

	@Schema(description = "상품 제목", example = "서울 한강 카약 체험")
	String productTitle,

	@Schema(description = "상품 썸네일 경로", example = "/images/product/thumbnail.jpg")
	String thumbnailPath,

	@Schema(description = "상품 가격", example = "35000")
	BigDecimal price,

	@Schema(description = "일정 날짜", example = "2026-05-01")
	LocalDate scheduleDt,

	@Schema(description = "시작 시간", example = "10:00")
	LocalTime startTime,

	@Schema(description = "종료 시간", example = "12:00")
	LocalTime endTime

) {
	public static FavoritesResponseDto from(Favorite favorite) {
		return new FavoritesResponseDto(
			favorite.getId(),
			favorite.getProductScheduleId(),
			favorite.getQuantity(),
			null,
			null,
			null,
			null,
			null,
			null,
			null
		);
	}

	public static FavoritesResponseDto from(Favorite favorite, Schedule schedule, Product product) {
		return new FavoritesResponseDto(
			favorite.getId(),
			favorite.getProductScheduleId(),
			favorite.getQuantity(),
			product.getId(),
			product.getTitle(),
			product.getThumbnailPath(),
			product.getPrice(),
			schedule.getScheduleDt(),
			schedule.getStartTime(),
			schedule.getEndTime()
		);
	}
}
