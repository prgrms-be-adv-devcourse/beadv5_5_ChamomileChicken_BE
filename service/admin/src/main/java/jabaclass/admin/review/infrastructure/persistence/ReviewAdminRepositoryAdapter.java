package jabaclass.admin.review.infrastructure.persistence;

import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import jabaclass.admin.review.domain.model.Review;
import jabaclass.admin.review.domain.repository.ReviewAdminRepository;
import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class ReviewAdminRepositoryAdapter implements ReviewAdminRepository {

	private final ReviewAdminJpaRepository reviewAdminJpaRepository;

	@Override
	public Optional<Review> findById(UUID reviewId) {
		return reviewAdminJpaRepository.findById(reviewId);
	}

	@Override
	public void deleteById(UUID reviewId) {
		reviewAdminJpaRepository.deleteById(reviewId);
	}
}
