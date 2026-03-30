package jabaclass.product.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import jabaclass.product.domain.model.Review;

public interface ReviewJpaRepository extends JpaRepository<Review, UUID> {

	Optional<Review> findByIdAndUserIdAndDeleteDtIsNull(UUID reviewId, UUID userId);

	List<Review> findByUserIdAndDeleteDtIsNull(UUID userId);

	List<Review> findByProductIdAndDeleteDtIsNull(UUID productId);
}
