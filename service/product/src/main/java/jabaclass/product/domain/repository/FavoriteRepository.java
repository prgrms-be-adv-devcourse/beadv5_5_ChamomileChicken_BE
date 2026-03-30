package jabaclass.product.domain.repository;

import java.util.List;
import java.util.UUID;

import jabaclass.product.domain.model.Favorite;

public interface FavoriteRepository {

	Favorite save(Favorite favorite);

	List<Favorite> findByUserIdAndDeleteDtIsNull(UUID id);

	Favorite findByIdAndUserIdAndDeleteDtIsNull(UUID favoriteId, UUID userId);
}
