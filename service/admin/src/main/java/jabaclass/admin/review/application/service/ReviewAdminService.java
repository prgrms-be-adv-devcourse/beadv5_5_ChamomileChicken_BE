package jabaclass.admin.review.application.service;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jabaclass.admin.common.error.AdminErrorCode;
import jabaclass.admin.common.error.BusinessException;
import jabaclass.admin.review.application.usecase.ReviewAdminUseCase;
import jabaclass.admin.review.domain.model.Review;
import jabaclass.admin.review.domain.repository.ReviewAdminRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ReviewAdminService implements ReviewAdminUseCase {

	private final ReviewAdminRepository reviewAdminRepository;

	@Override
	@Transactional
	public void deleteReview(UUID reviewId) {
		Review review = reviewAdminRepository.findById(reviewId)
			.orElseThrow(() -> new BusinessException(AdminErrorCode.REVIEW_NOT_FOUND));
		review.softDelete();
	}
}
