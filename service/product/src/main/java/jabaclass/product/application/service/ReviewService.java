package jabaclass.product.application.service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jabaclass.product.application.acl.SellerRepository;
import jabaclass.product.application.exception.BusinessException;
import jabaclass.product.application.usecase.ReviewUseCase;
import jabaclass.product.common.exception.CommonErrorCode;
import jabaclass.product.domain.model.Review;
import jabaclass.product.domain.repository.ReviewRepository;
import jabaclass.product.infrastructure.acl.dto.response.UserResponseDto;
import jabaclass.product.presentation.dto.request.ReviewRequestDto;
import jabaclass.product.presentation.dto.response.ReviewResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
@Slf4j
public class ReviewService implements ReviewUseCase {

	private final ReviewRepository reviewRepository;
	private final SellerRepository sellerRepository;

	@Override
	@Transactional
	public ReviewResponseDto createReview(ReviewRequestDto review, UUID productId, UUID userId) {

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
	public ReviewResponseDto updateReview(ReviewRequestDto requestDto, UUID revewId, UUID userId) {

		// 본인 리뷰인지 매치
		Review review = findByUserId(userId, revewId);

		// 수정
		review.changeRating(requestDto.rating());
		review.changeContent(requestDto.content());

		return ReviewResponseDto.from(review);
	}

	@Override
	@Transactional
	public void deleteReview(UUID reviewId, UUID userId) {

		// 본인 리뷰인지 매치
		Review review = findByUserId(userId, reviewId);

		review.changeDelete();

	}

	@Override
	public List<ReviewResponseDto> userReview(UUID userId) {

		List<Review> reviews = reviewRepository.findByUserIdAndDeleteDtIsNull(userId);

		Map<UUID, String> userNameMap = getUserNameMap(
			reviews.stream().map(Review::getUserId).distinct().toList()
		);

		return reviews.stream()
			.map(review -> ReviewResponseDto.from(review, userNameMap.get(review.getUserId())))
			.toList();
	}

	@Override
	public List<ReviewResponseDto> productReview(UUID productId) {
		List<Review> reviews = reviewRepository.findByProductIdAndDeleteDtIsNull(productId);

		Map<UUID, String> userNameMap = getUserNameMap(
			reviews.stream().map(Review::getUserId).distinct().toList()
		);

		return reviews.stream()
			.map(review -> ReviewResponseDto.from(review, userNameMap.get(review.getUserId())))
			.toList();
	}

	@Override
	public ReviewResponseDto review(UUID reviewId) {
		Review review = reviewRepository.findById(reviewId)
			.orElseThrow(() -> new BusinessException(CommonErrorCode.NOT_FOUND_REVIEW));

		return ReviewResponseDto.from(review);
	}

	private Map<UUID, String> getUserNameMap(List<UUID> userIds) {
		if (userIds.isEmpty()) {
			return Collections.emptyMap();
		}
		try {
			return sellerRepository.findSellerList(userIds)
				.orElse(Collections.emptyList())
				.stream()
				.collect(Collectors.toMap(UserResponseDto::userId, UserResponseDto::name));
		} catch (Exception e) {
			log.warn("사용자 정보 조회 실패, userName을 null로 처리합니다.", e);
			return Collections.emptyMap();
		}
	}

	private Review findByUserId(UUID userId, UUID revewId) {
		Review review = reviewRepository.findByIdAndUserIdAndDeleteDtIsNull(revewId, userId)
			.orElseThrow(() -> new BusinessException(CommonErrorCode.NOT_MATCH_USER_REVIEW));

		return review;
	}
}
