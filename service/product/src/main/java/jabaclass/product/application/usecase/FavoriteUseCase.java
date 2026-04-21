package jabaclass.product.application.usecase;

import java.util.List;
import java.util.UUID;

import jabaclass.product.presentation.dto.response.FavoritesResponseDto;

public interface FavoriteUseCase {

	FavoritesResponseDto createFavorite(int quantity, UUID scheduleId, UUID userId);

	void deleteFavorite(UUID favoriteId, UUID userId);

	List<FavoritesResponseDto> findByUserIdAndDeleteDtIsNull(UUID userId);
}
