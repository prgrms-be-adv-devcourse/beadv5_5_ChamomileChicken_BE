package jabaclass.admin.review.application.service;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jabaclass.admin.common.error.AdminErrorCode;
import jabaclass.admin.common.error.BusinessException;
import jabaclass.admin.review.application.usecase.ReviewAdminUseCase;
import jabaclass.admin.review.domain.model.Review;
import jabaclass.admin.review.domain.repository.ReviewAdminRepository;
import jabaclass.admin.review.presentation.dto.response.ReviewAdminResponseDto;
import jabaclass.admin.user.domain.repository.UserAdminRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ReviewAdminService implements ReviewAdminUseCase {

	private final ReviewAdminRepository reviewAdminRepository;
	private final UserAdminRepository userAdminRepository;

	@Override
	@Transactional(readOnly = true)
	public Page<ReviewAdminResponseDto> getReviews(Pageable pageable) {
		Page<Review> reviews = reviewAdminRepository.findAll(pageable);

		Set<UUID> userIds = reviews.stream()
			.map(Review::getUserId)
			.collect(Collectors.toSet());

		Map<UUID, String> emailMap = userAdminRepository.findEmailsByIds(userIds);

		return reviews.map(review ->
			ReviewAdminResponseDto.from(review, emailMap.getOrDefault(review.getUserId(), ""))
		);
	}

	@Override
	@Transactional
	public void deleteReview(UUID reviewId) {
		Review review = reviewAdminRepository.findById(reviewId)
			.orElseThrow(() -> new BusinessException(AdminErrorCode.REVIEW_NOT_FOUND));
		review.softDelete();
	}
}
