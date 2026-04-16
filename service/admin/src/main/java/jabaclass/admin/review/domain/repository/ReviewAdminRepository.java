package jabaclass.admin.review.domain.repository;

import java.util.Optional;
import java.util.UUID;

import jabaclass.admin.review.domain.model.Review;

public interface ReviewAdminRepository {
	Optional<Review> findById(UUID reviewId);
	void deleteById(UUID reviewId);
}
