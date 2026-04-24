package jabaclass.admin.review.domain.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import jabaclass.admin.review.domain.model.Review;

public interface ReviewAdminRepository {
	Page<Review> findAll(Pageable pageable);
	Optional<Review> findById(UUID reviewId);
	void deleteById(UUID reviewId);
}
