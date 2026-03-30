package jabaclass.product.application.usecase;

import java.util.List;
import java.util.UUID;

import jabaclass.product.presentation.dto.respose.FavoritesResposeDto;

public interface FavoriteUseCase {

	FavoritesResposeDto createFavorite(int quantity, UUID scheduleId);

	void deleteFavorite(UUID favoriteId);

	List<FavoritesResposeDto> findByUserIdAndDeleteDtIsNull();
}
