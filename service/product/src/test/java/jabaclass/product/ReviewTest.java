package jabaclass.product;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;

import jabaclass.product.application.exception.BusinessException;
import jabaclass.product.application.service.AuditorAwareService;
import jabaclass.product.application.service.ReviewService;
import jabaclass.product.application.usecase.ReviewUseCase;
import jabaclass.product.common.exception.ApiResponseDto;
import jabaclass.product.common.exception.CommonErrorCode;
import jabaclass.product.domain.model.Review;
import jabaclass.product.domain.repository.ReviewRepository;
import jabaclass.product.presentation.controller.ReviewRestController;
import jabaclass.product.presentation.dto.request.ReviewRequestDto;
import jabaclass.product.presentation.dto.respose.ReviewResponseDto;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;

@ExtendWith(MockitoExtension.class)
class ReviewTest {

	@InjectMocks
	private ReviewService reviewService;

	@InjectMocks
	private ReviewRestController reviewRestController;

	@Mock
	private ReviewRepository reviewRepository;

	@Mock
	private AuditorAwareService auditorAwareService;

	@Mock
	private ReviewUseCase reviewUseCase;

	private static final UUID USER_ID = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
	private static final UUID PRODUCT_ID = UUID.fromString("223e4567-e89b-12d3-a456-426614174000");
	private static final UUID REVIEW_ID = UUID.fromString("323e4567-e89b-12d3-a456-426614174000");

	private Validator validator;
	private Review review;
	private ReviewRequestDto request;

	@BeforeEach
	void setUp() {
		validator = Validation.buildDefaultValidatorFactory().getValidator();

		review = Review.builder()
			.productId(PRODUCT_ID)
			.userId(USER_ID)
			.rating(5)
			.content("좋아요")
			.build();
		ReflectionTestUtils.setField(review, "id", REVIEW_ID);

		request = new ReviewRequestDto(5, "좋아요");
	}

	@Test
	void 리뷰를_생성한다() {
		given(auditorAwareService.getCurrentAuditor()).willReturn(Optional.of(USER_ID));
		given(reviewRepository.save(any(Review.class)))
			.willAnswer(invocation -> {
				Review saved = invocation.getArgument(0);
				ReflectionTestUtils.setField(saved, "id", REVIEW_ID);
				return saved;
			});

		ReviewResponseDto result = reviewService.createReview(request, PRODUCT_ID);

		assertThat(result.id()).isEqualTo(REVIEW_ID);
		assertThat(result.rating()).isEqualTo(5);
		assertThat(result.content()).isEqualTo("좋아요");
	}

	@Test
	void 리뷰를_수정한다() {
		ReviewRequestDto updateRequest = new ReviewRequestDto(3, "보통이에요");
		given(auditorAwareService.getCurrentAuditor()).willReturn(Optional.of(USER_ID));
		given(reviewRepository.findByIdAndUserIdAndDeleteDtIsNull(REVIEW_ID, USER_ID)).willReturn(Optional.of(review));

		ReviewResponseDto result = reviewService.updateReview(updateRequest, REVIEW_ID);

		assertThat(result.rating()).isEqualTo(3);
		assertThat(result.content()).isEqualTo("보통이에요");
	}

	@Test
	void 리뷰를_삭제한다() {
		given(auditorAwareService.getCurrentAuditor()).willReturn(Optional.of(USER_ID));
		given(reviewRepository.findByIdAndUserIdAndDeleteDtIsNull(REVIEW_ID, USER_ID)).willReturn(Optional.of(review));

		reviewService.deleteReview(REVIEW_ID);

		assertThat(review.getDeleteDt()).isNotNull();
	}

	@Test
	void 내_리뷰_목록을_조회한다() {
		given(auditorAwareService.getCurrentAuditor()).willReturn(Optional.of(USER_ID));
		given(reviewRepository.findByUserIdAndDeleteDtIsNull(USER_ID)).willReturn(List.of(review));

		List<ReviewResponseDto> result = reviewService.userReview();

		assertThat(result).hasSize(1);
		assertThat(result.get(0).id()).isEqualTo(REVIEW_ID);
	}

	@Test
	void 상품_리뷰_목록을_조회한다() {
		given(reviewRepository.findByProductIdAndDeleteDtIsNull(PRODUCT_ID)).willReturn(List.of(review));

		List<ReviewResponseDto> result = reviewService.prodcutReview(PRODUCT_ID);

		assertThat(result).hasSize(1);
		assertThat(result.get(0).content()).isEqualTo("좋아요");
	}

	@Test
	void 리뷰를_단건_조회한다() {
		given(reviewRepository.findById(REVIEW_ID)).willReturn(Optional.of(review));

		ReviewResponseDto result = reviewService.reivew(REVIEW_ID);

		assertThat(result.id()).isEqualTo(REVIEW_ID);
		assertThat(result.rating()).isEqualTo(5);
	}

	@Test
	void 로그인_사용자가_없으면_리뷰_생성에_실패한다() {
		given(auditorAwareService.getCurrentAuditor()).willReturn(Optional.empty());

		assertThatThrownBy(() -> reviewService.createReview(request, PRODUCT_ID))
			.isInstanceOf(BusinessException.class)
			.hasMessage(CommonErrorCode.EMPTY_USER.getMessage());
	}

	@Test
	void 리뷰_DTO의_평점이_범위를_벗어나면_검증에_실패한다() {
		ReviewRequestDto invalidRequest = new ReviewRequestDto(0, "별로예요");

		Set<ConstraintViolation<ReviewRequestDto>> violations = validator.validate(invalidRequest);

		assertThat(violations).extracting(ConstraintViolation::getPropertyPath)
			.anyMatch(path -> path.toString().equals("rating"));
	}

	@Test
	void 리뷰_생성_요청이_들어오면_컨트롤러가_유스케이스를_호출한다() {
		ReviewResponseDto response = new ReviewResponseDto(REVIEW_ID, 5, "좋아요");
		given(reviewUseCase.createReview(request, PRODUCT_ID)).willReturn(response);

		ResponseEntity<ApiResponseDto<ReviewResponseDto>> result = reviewRestController.create(request, PRODUCT_ID);

		assertThat(result.getStatusCode()).isEqualTo(HttpStatus.CREATED);
		assertThat(result.getBody()).isNotNull();
		assertThat(result.getBody().getData()).isEqualTo(response);
		then(reviewUseCase).should().createReview(request, PRODUCT_ID);
	}

	@Test
	void 리뷰_수정_요청이_들어오면_컨트롤러가_유스케이스를_호출한다() {
		ReviewResponseDto response = new ReviewResponseDto(REVIEW_ID, 3, "보통이에요");
		ReviewRequestDto updateRequest = new ReviewRequestDto(3, "보통이에요");
		given(reviewUseCase.updateReview(updateRequest, REVIEW_ID)).willReturn(response);

		ResponseEntity<ApiResponseDto<ReviewResponseDto>> result = reviewRestController.update(updateRequest,
			REVIEW_ID);

		assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(result.getBody()).isNotNull();
		assertThat(result.getBody().getData()).isEqualTo(response);
		then(reviewUseCase).should().updateReview(updateRequest, REVIEW_ID);
	}

	@Test
	void 리뷰_삭제_요청이_들어오면_컨트롤러가_유스케이스를_호출한다() {
		ResponseEntity<ApiResponseDto<UUID>> result = reviewRestController.delete(REVIEW_ID);

		assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
		then(reviewUseCase).should().deleteReview(REVIEW_ID);
	}

	@Test
	void 내_리뷰_조회_요청이_들어오면_컨트롤러가_유스케이스를_호출한다() {
		List<ReviewResponseDto> response = List.of(new ReviewResponseDto(REVIEW_ID, 5, "좋아요"));
		given(reviewUseCase.userReview()).willReturn(response);

		ResponseEntity<ApiResponseDto<List<ReviewResponseDto>>> result = reviewRestController.userReview();

		assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(result.getBody()).isNotNull();
		assertThat(result.getBody().getData()).hasSize(1);
		then(reviewUseCase).should().userReview();
	}

	@Test
	void 상품_리뷰_조회_요청이_들어오면_컨트롤러가_유스케이스를_호출한다() {
		List<ReviewResponseDto> response = List.of(new ReviewResponseDto(REVIEW_ID, 5, "좋아요"));
		given(reviewUseCase.prodcutReview(PRODUCT_ID)).willReturn(response);

		ResponseEntity<ApiResponseDto<List<ReviewResponseDto>>> result = reviewRestController.productReview(PRODUCT_ID);

		assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(result.getBody()).isNotNull();
		assertThat(result.getBody().getData()).hasSize(1);
		then(reviewUseCase).should().prodcutReview(PRODUCT_ID);
	}

	@Test
	void 리뷰_단건_조회_요청이_들어오면_컨트롤러가_유스케이스를_호출한다() {
		ReviewResponseDto response = new ReviewResponseDto(REVIEW_ID, 5, "좋아요");
		given(reviewUseCase.reivew(REVIEW_ID)).willReturn(response);

		ResponseEntity<ApiResponseDto<ReviewResponseDto>> result = reviewRestController.review(REVIEW_ID);

		assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(result.getBody()).isNotNull();
		assertThat(result.getBody().getData()).isEqualTo(response);
		then(reviewUseCase).should().reivew(REVIEW_ID);
	}
}
