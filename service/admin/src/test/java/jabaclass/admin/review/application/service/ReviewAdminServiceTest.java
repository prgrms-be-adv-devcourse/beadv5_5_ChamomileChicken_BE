package jabaclass.admin.review.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator.ReplaceUnderscores;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import jabaclass.admin.common.error.BusinessException;
import jabaclass.admin.review.domain.model.Review;
import jabaclass.admin.review.domain.repository.ReviewAdminRepository;

@SuppressWarnings("NonAsciiCharacters")
@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(ReplaceUnderscores.class)
class ReviewAdminServiceTest {

	@Mock
	private ReviewAdminRepository reviewAdminRepository;

	@InjectMocks
	private ReviewAdminService reviewAdminService;

	private UUID reviewId;
	private Review review;

	@BeforeEach
	void setUp() {
		reviewId = UUID.randomUUID();
		review = Review.builder()
			.productId(UUID.randomUUID())
			.userId(UUID.randomUUID())
			.rating(1)
			.content("부적절한 리뷰 내용")
			.build();
		ReflectionTestUtils.setField(review, "id", reviewId);
	}

	@Test
	void 부적절한_리뷰를_삭제한다() {
		// given
		given(reviewAdminRepository.findById(reviewId)).willReturn(Optional.of(review));

		// when
		reviewAdminService.deleteReview(reviewId);

		// then
		assertThat(review.getDeleteDt()).isNotNull();
		then(reviewAdminRepository).shouldHaveNoMoreInteractions();
	}

	@Test
	void 존재하지_않는_리뷰_삭제시_예외가_발생한다() {
		// given
		given(reviewAdminRepository.findById(reviewId)).willReturn(Optional.empty());

		// when & then
		assertThatThrownBy(() -> reviewAdminService.deleteReview(reviewId))
			.isInstanceOf(BusinessException.class)
			.hasMessage("리뷰를 찾을 수 없습니다.");
		then(reviewAdminRepository).shouldHaveNoMoreInteractions();
	}
}
