package jabaclass.admin.review.infrastructure.persistence;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import jabaclass.admin.review.domain.model.Review;

public interface ReviewAdminJpaRepository extends JpaRepository<Review, UUID> {
}
