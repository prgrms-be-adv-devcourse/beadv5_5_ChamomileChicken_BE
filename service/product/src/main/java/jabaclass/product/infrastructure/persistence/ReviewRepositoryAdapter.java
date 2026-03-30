package jabaclass.product.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import jabaclass.product.domain.model.Review;
import jabaclass.product.domain.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class ReviewRepositoryAdapter implements ReviewRepository {
	private final ReviewJpaRepository reviewJpaRepository;

	@Override
	public Review save(Review review) {
		return reviewJpaRepository.save(review);
	}

	@Override
	public Optional<Review> findByIdAndUserIdAndDeleteDtIsNull(UUID reviewId, UUID userId) {
		return reviewJpaRepository.findByIdAndUserIdAndDeleteDtIsNull(reviewId, userId);
	}

	@Override
	public List<Review> findByUserIdAndDeleteDtIsNull(UUID review) {
		return reviewJpaRepository.findByUserIdAndDeleteDtIsNull(review);
	}

	@Override
	public List<Review> findByProductIdAndDeleteDtIsNull(UUID productId) {
		return reviewJpaRepository.findByProductIdAndDeleteDtIsNull(productId);
	}

	@Override
	public Optional<Review> findById(UUID id) {
		return reviewJpaRepository.findById(id);
	}
}
