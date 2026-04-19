package jabaclass.product.application.usecase;

import java.util.List;
import java.util.UUID;

import jabaclass.product.presentation.dto.response.FavoritesResposeDto;

public interface FavoriteUseCase {

	FavoritesResposeDto createFavorite(int quantity, UUID scheduleId, UUID userId);

	void deleteFavorite(UUID favoriteId, UUID userId);

	List<FavoritesResposeDto> findByUserIdAndDeleteDtIsNull(UUID userId);
}
