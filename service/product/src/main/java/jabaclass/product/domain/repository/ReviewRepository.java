package jabaclass.product.domain.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import jabaclass.product.domain.model.Review;

public interface ReviewRepository {

	Review save(Review review);

	Optional<Review> findByIdAndUserIdAndDeleteDtIsNull(UUID reviewId, UUID userId);

	List<Review> findByUserIdAndDeleteDtIsNull(UUID review);

	List<Review> findByProductIdAndDeleteDtIsNull(UUID review);

	Optional<Review> findById(UUID id);
}
