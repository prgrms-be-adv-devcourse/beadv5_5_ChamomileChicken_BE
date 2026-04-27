package jabaclass.product;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import jabaclass.product.application.usecase.ReviewUseCase;
import jabaclass.product.common.exception.ApiResponseDto;
import jabaclass.product.presentation.controller.ReviewRestController;
import jabaclass.product.presentation.dto.request.ReviewRequestDto;
import jabaclass.product.presentation.dto.response.ReviewResponseDto;

@ExtendWith(MockitoExtension.class)
class ReviewRestControllerTest {

	@InjectMocks
	private ReviewRestController reviewRestController;

	@Mock
	private ReviewUseCase reviewUseCase;

	private static final UUID PRODUCT_ID = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
	private static final UUID REVIEW_ID = UUID.fromString("223e4567-e89b-12d3-a456-426614174000");
	private static final UUID USER_ID = UUID.fromString("323e4567-e89b-12d3-a456-426614174000");

	private ReviewRequestDto request;

	@BeforeEach
	void setUp() {
		request = new ReviewRequestDto(5, "좋았어요");
	}

	@Test
	void 리뷰_생성_요청이_들어오면_유스케이스를_호출한다() {
		ReviewResponseDto response = new ReviewResponseDto(REVIEW_ID, 5, "좋았어요", null, null);
		given(reviewUseCase.createReview(request, PRODUCT_ID, USER_ID)).willReturn(response);

		ResponseEntity<ApiResponseDto<ReviewResponseDto>> result = reviewRestController.create(request, PRODUCT_ID, USER_ID);

		assertThat(result.getStatusCode()).isEqualTo(HttpStatus.CREATED);
		assertThat(result.getBody()).isNotNull();
		assertThat(result.getBody().getData()).isEqualTo(response);
		then(reviewUseCase).should().createReview(request, PRODUCT_ID, USER_ID);
	}

	@Test
	void 리뷰_수정_요청이_들어오면_유스케이스를_호출한다() {
		ReviewRequestDto updateRequest = new ReviewRequestDto(3, "보통이었어요");
		ReviewResponseDto response = new ReviewResponseDto(REVIEW_ID, 3, "보통이었어요", null, null);
		given(reviewUseCase.updateReview(updateRequest, REVIEW_ID, USER_ID)).willReturn(response);

		ResponseEntity<ApiResponseDto<ReviewResponseDto>> result = reviewRestController.update(updateRequest, REVIEW_ID, USER_ID);

		assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(result.getBody()).isNotNull();
		assertThat(result.getBody().getData()).isEqualTo(response);
		then(reviewUseCase).should().updateReview(updateRequest, REVIEW_ID, USER_ID);
	}

	@Test
	void 리뷰_삭제_요청이_들어오면_유스케이스를_호출한다() {
		ResponseEntity<ApiResponseDto<UUID>> result = reviewRestController.delete(REVIEW_ID, USER_ID);

		assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
		then(reviewUseCase).should().deleteReview(REVIEW_ID, USER_ID);
	}

	@Test
	void 내_리뷰_조회_요청이_들어오면_유스케이스를_호출한다() {
		List<ReviewResponseDto> response = List.of(new ReviewResponseDto(REVIEW_ID, 5, "좋았어요", null, null));
		given(reviewUseCase.userReview(USER_ID)).willReturn(response);

		ResponseEntity<ApiResponseDto<List<ReviewResponseDto>>> result = reviewRestController.userReview(USER_ID);

		assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(result.getBody()).isNotNull();
		assertThat(result.getBody().getData()).hasSize(1);
		then(reviewUseCase).should().userReview(USER_ID);
	}

	@Test
	void 상품_리뷰_조회_요청이_들어오면_유스케이스를_호출한다() {
		List<ReviewResponseDto> response = List.of(new ReviewResponseDto(REVIEW_ID, 5, "좋았어요", null, null));
		given(reviewUseCase.productReview(PRODUCT_ID)).willReturn(response);

		ResponseEntity<ApiResponseDto<List<ReviewResponseDto>>> result = reviewRestController.productReview(PRODUCT_ID);

		assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(result.getBody()).isNotNull();
		assertThat(result.getBody().getData()).hasSize(1);
		then(reviewUseCase).should().productReview(PRODUCT_ID);
	}

	@Test
	void 리뷰_단건_조회_요청이_들어오면_유스케이스를_호출한다() {
		ReviewResponseDto response = new ReviewResponseDto(REVIEW_ID, 5, "좋았어요", null, null);
		given(reviewUseCase.review(REVIEW_ID)).willReturn(response);

		ResponseEntity<ApiResponseDto<ReviewResponseDto>> result = reviewRestController.review(REVIEW_ID);

		assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(result.getBody()).isNotNull();
		assertThat(result.getBody().getData()).isEqualTo(response);
		then(reviewUseCase).should().review(REVIEW_ID);
	}
}
