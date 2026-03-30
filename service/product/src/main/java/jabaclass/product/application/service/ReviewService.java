package jabaclass.product.application.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jabaclass.product.application.exception.BusinessException;
import jabaclass.product.application.usecase.ReviewUseCase;
import jabaclass.product.common.exception.CommonErrorCode;
import jabaclass.product.domain.model.Review;
import jabaclass.product.domain.repository.ReviewRepository;
import jabaclass.product.presentation.dto.request.ReviewRequestDto;
import jabaclass.product.presentation.dto.respose.ReviewResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
@Slf4j
public class ReviewService implements ReviewUseCase {

	private final ReviewRepository reviewRepository;
	private final AuditorAwareService auditorAwareService;

	@Override
	@Transactional
	public ReviewResponseDto createReview(ReviewRequestDto review, UUID productId) {
		UUID userId = auditorAwareService.getCurrentAuditor()
			.orElseThrow(() -> new BusinessException(CommonErrorCode.EMPTY_USER));

		Review entity = Review.builder()
			.productId(productId)
			.userId(userId)
			.rating(review.rating())
			.content(review.content())
			.build();

		reviewRepository.save(entity);

		return ReviewResponseDto.from(entity);
	}

	@Override
	@Transactional
	public ReviewResponseDto updateReview(ReviewRequestDto requestDto, UUID revewId) {
		UUID userId = auditorAwareService.getCurrentAuditor()
			.orElseThrow(() -> new BusinessException(CommonErrorCode.EMPTY_USER));

		// 본인 리뷰인지 매치
		Review review = findByUserId(userId, revewId);

		// 수정
		review.changeRating(requestDto.rating());
		review.changeContent(requestDto.content());

		return ReviewResponseDto.from(review);
	}

	@Override
	@Transactional
	public void deleteReview(UUID reviewId) {
		UUID userId = auditorAwareService.getCurrentAuditor()
			.orElseThrow(() -> new BusinessException(CommonErrorCode.EMPTY_USER));

		// 본인 리뷰인지 매치
		Review review = findByUserId(userId, reviewId);

		review.changeDelete();

	}

	@Override
	public List<ReviewResponseDto> userReview() {
		UUID userId = auditorAwareService.getCurrentAuditor()
			.orElseThrow(() -> new BusinessException(CommonErrorCode.EMPTY_USER));

		List<Review> reviews = reviewRepository.findByUserIdAndDeleteDtIsNull(userId);

		return reviews.stream()
			.map(ReviewResponseDto::from)
			.toList();
	}

	@Override
	public List<ReviewResponseDto> productReview(UUID productId) {
		List<Review> reviews = reviewRepository.findByProductIdAndDeleteDtIsNull(productId);

		return reviews.stream()
			.map(ReviewResponseDto::from)
			.toList();
	}

	@Override
	public ReviewResponseDto review(UUID reviewId) {
		Review review = reviewRepository.findById(reviewId)
			.orElseThrow(() -> new BusinessException(CommonErrorCode.NOT_FOUND_REVIEW));

		return ReviewResponseDto.from(review);
	}

	private Review findByUserId(UUID userId, UUID revewId) {
		Review review = reviewRepository.findByIdAndUserIdAndDeleteDtIsNull(revewId, userId)
			.orElseThrow(() -> new BusinessException(CommonErrorCode.NOT_MATCH_USER_REVIEW));

		return review;
	}
}
