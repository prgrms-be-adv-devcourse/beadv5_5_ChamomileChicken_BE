package jabaclass.product.infrastructure.persistence;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import jabaclass.product.domain.model.Favorite;

public interface FavoriteJpaRepository extends JpaRepository<Favorite, UUID> {

	List<Favorite> findByUserIdAndDeleteDtIsNull(UUID id);

	Favorite findByIdAndUserIdAndDeleteDtIsNull(UUID favoriteId, UUID userId);
}
