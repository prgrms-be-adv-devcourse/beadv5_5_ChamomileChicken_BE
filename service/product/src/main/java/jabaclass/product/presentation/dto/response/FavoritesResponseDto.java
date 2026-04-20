package jabaclass.product.presentation.dto.response;

import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;
import jabaclass.product.domain.model.Favorite;

@Schema(description = "즐겨 찾기")
public record FavoritesResponseDto(

	@Schema(description = "즐겨찾기 Id", example = "550e8400-e29b-41d4-a716-446655440000")
	UUID id,

	@Schema(description = "상품 일정 Id", example = "550e8400-e29b-41d4-a716-446655440000")
	UUID productScheduleId,

	@Schema(description = "수량", example = "2")
	int quantity

) {
	public static FavoritesResponseDto from(Favorite favorite) {
		return new FavoritesResponseDto(
			favorite.getId(),
			favorite.getProductScheduleId(),
			favorite.getQuantity()
		);
	}
}
