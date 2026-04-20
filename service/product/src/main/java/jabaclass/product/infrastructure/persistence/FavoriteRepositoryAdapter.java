package jabaclass.product.infrastructure.persistence;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import jabaclass.product.domain.model.Favorite;
import jabaclass.product.domain.repository.FavoriteRepository;
import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class FavoriteRepositoryAdapter implements FavoriteRepository {

	private final FavoriteJpaRepository favoriteJpaRepository;

	@Override
	public Favorite save(Favorite favorite) {
		return favoriteJpaRepository.save(favorite);
	}

	@Override
	public List<Favorite> findByUserIdAndDeleteDtIsNull(UUID id) {
		return favoriteJpaRepository.findByUserIdAndDeleteDtIsNull(id);
	}

	@Override
	public Favorite findByIdAndUserIdAndDeleteDtIsNull(UUID favoriteId, UUID userId) {
		return favoriteJpaRepository.findByIdAndUserIdAndDeleteDtIsNull(favoriteId, userId);
	}
}
