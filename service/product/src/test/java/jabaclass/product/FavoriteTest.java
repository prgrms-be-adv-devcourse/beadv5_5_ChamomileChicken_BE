package jabaclass.product;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
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
import org.springframework.test.util.ReflectionTestUtils;

import jabaclass.product.application.exception.BusinessException;
import jabaclass.product.application.service.FavoriteService;
import jabaclass.product.application.usecase.FavoriteUseCase;
import jabaclass.product.common.exception.ApiResponseDto;
import jabaclass.product.common.exception.CommonErrorCode;
import jabaclass.product.domain.model.Favorite;
import jabaclass.product.domain.model.Product;
import jabaclass.product.domain.model.Schedule;
import jabaclass.product.domain.model.status.ProductStatus;
import jabaclass.product.domain.model.status.ReservedStatus;
import jabaclass.product.domain.repository.FavoriteRepository;
import jabaclass.product.domain.repository.ProductRepository;
import jabaclass.product.domain.repository.ScheduleRepository;
import jabaclass.product.presentation.controller.FavoritesRestController;
import jabaclass.product.presentation.dto.response.FavoritesResponseDto;

@ExtendWith(MockitoExtension.class)
class FavoriteTest {

	@InjectMocks
	private FavoriteService favoriteService;

	@InjectMocks
	private FavoritesRestController favoritesRestController;

	@Mock
	private FavoriteRepository favoriteRepository;

	@Mock
	private ScheduleRepository scheduleRepository;

	@Mock
	private ProductRepository productRepository;

	@Mock
	private FavoriteUseCase favoriteUseCase;

	private static final UUID USER_ID = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
	private static final UUID SCHEDULE_ID = UUID.fromString("223e4567-e89b-12d3-a456-426614174000");
	private static final UUID FAVORITE_ID = UUID.fromString("323e4567-e89b-12d3-a456-426614174000");
	private static final UUID PRODUCT_ID = UUID.fromString("423e4567-e89b-12d3-a456-426614174000");

	private Favorite favorite;

	@BeforeEach
	void setUp() {
		favorite = Favorite.builder()
			.productScheduleId(SCHEDULE_ID)
			.userId(USER_ID)
			.quantity(2)
			.build();
		ReflectionTestUtils.setField(favorite, "id", FAVORITE_ID);
	}

	@Test
	void 즐겨찾기를_생성한다() {
		given(favoriteRepository.save(any(Favorite.class))).willAnswer(invocation -> {
			Favorite saved = invocation.getArgument(0);
			ReflectionTestUtils.setField(saved, "id", FAVORITE_ID);
			return saved;
		});

		FavoritesResponseDto result = favoriteService.createFavorite(2, SCHEDULE_ID, USER_ID);

		assertThat(result.id()).isEqualTo(FAVORITE_ID);
		assertThat(result.productScheduleId()).isEqualTo(SCHEDULE_ID);
		assertThat(result.quantity()).isEqualTo(2);
	}

	@Test
	void 즐겨찾기를_삭제한다() {
		given(favoriteRepository.findByIdAndUserIdAndDeleteDtIsNull(FAVORITE_ID, USER_ID)).willReturn(favorite);

		favoriteService.deleteFavorite(FAVORITE_ID, USER_ID);

		assertThat(favorite.getDeleteDt()).isNotNull();
	}

	@Test
	void 찜목록_조회_성공_상품정보_포함() {
		// given
		Schedule schedule = Schedule.builder()
			.productId(PRODUCT_ID)
			.scheduleDt(LocalDate.of(2026, 5, 1))
			.startTime(LocalTime.of(10, 0))
			.endTime(LocalTime.of(12, 0))
			.status(ReservedStatus.AVAILABLE)
			.capacity(10)
			.build();
		ReflectionTestUtils.setField(schedule, "id", SCHEDULE_ID);

		Product product = Product.builder()
			.sellerId(USER_ID)
			.title("테스트 상품")
			.thumbnailPath("/images/test.jpg")
			.price(BigDecimal.valueOf(35000))
			.maxCapacity(10)
			.description("테스트 설명")
			.status(ProductStatus.ENABLE)
			.roadAddress("서울시 강남구")
			.latitude(BigDecimal.valueOf(37.5))
			.longitude(BigDecimal.valueOf(127.0))
			.build();
		ReflectionTestUtils.setField(product, "id", PRODUCT_ID);

		given(favoriteRepository.findByUserIdAndDeleteDtIsNull(USER_ID)).willReturn(List.of(favorite));
		given(scheduleRepository.findAllByIdInAndDeleteDtIsNull(List.of(SCHEDULE_ID))).willReturn(List.of(schedule));
		given(productRepository.findAllByIdsAndDeleteDtIsNull(List.of(PRODUCT_ID))).willReturn(List.of(product));

		// when
		List<FavoritesResponseDto> result = favoriteService.findByUserIdAndDeleteDtIsNull(USER_ID);

		// then
		assertThat(result).hasSize(1);
		FavoritesResponseDto dto = result.get(0);
		assertThat(dto.id()).isEqualTo(FAVORITE_ID);
		assertThat(dto.productScheduleId()).isEqualTo(SCHEDULE_ID);
		assertThat(dto.quantity()).isEqualTo(2);
		assertThat(dto.productId()).isEqualTo(PRODUCT_ID);
		assertThat(dto.productTitle()).isEqualTo("테스트 상품");
		assertThat(dto.thumbnailPath()).isEqualTo("/images/test.jpg");
		assertThat(dto.price()).isEqualByComparingTo(BigDecimal.valueOf(35000));
		assertThat(dto.scheduleDt()).isEqualTo(LocalDate.of(2026, 5, 1));
		assertThat(dto.startTime()).isEqualTo(LocalTime.of(10, 0));
		assertThat(dto.endTime()).isEqualTo(LocalTime.of(12, 0));
	}

	@Test
	void 찜목록_조회_일정없음_해당항목_제외() {
		// given
		given(favoriteRepository.findByUserIdAndDeleteDtIsNull(USER_ID)).willReturn(List.of(favorite));
		given(scheduleRepository.findAllByIdInAndDeleteDtIsNull(List.of(SCHEDULE_ID))).willReturn(List.of());
		given(productRepository.findAllByIdsAndDeleteDtIsNull(List.of())).willReturn(List.of());

		// when
		List<FavoritesResponseDto> result = favoriteService.findByUserIdAndDeleteDtIsNull(USER_ID);

		// then
		assertThat(result).isEmpty();
	}

	@Test
	void 찜목록_조회_상품없음_해당항목_제외() {
		// given
		Schedule schedule = Schedule.builder()
			.productId(PRODUCT_ID)
			.scheduleDt(LocalDate.of(2026, 5, 1))
			.startTime(LocalTime.of(10, 0))
			.endTime(LocalTime.of(12, 0))
			.status(ReservedStatus.AVAILABLE)
			.capacity(10)
			.build();
		ReflectionTestUtils.setField(schedule, "id", SCHEDULE_ID);

		given(favoriteRepository.findByUserIdAndDeleteDtIsNull(USER_ID)).willReturn(List.of(favorite));
		given(scheduleRepository.findAllByIdInAndDeleteDtIsNull(List.of(SCHEDULE_ID))).willReturn(List.of(schedule));
		given(productRepository.findAllByIdsAndDeleteDtIsNull(List.of(PRODUCT_ID))).willReturn(List.of());

		// when
		List<FavoritesResponseDto> result = favoriteService.findByUserIdAndDeleteDtIsNull(USER_ID);

		// then
		assertThat(result).isEmpty();
	}

	@Test
	void 찜목록_조회_여러건중_일부만_유효한_경우() {
		// given
		UUID scheduleId2 = UUID.fromString("523e4567-e89b-12d3-a456-426614174000");
		UUID favoriteId2 = UUID.fromString("623e4567-e89b-12d3-a456-426614174000");

		Favorite favorite2 = Favorite.builder()
			.productScheduleId(scheduleId2)
			.userId(USER_ID)
			.quantity(1)
			.build();
		ReflectionTestUtils.setField(favorite2, "id", favoriteId2);

		Schedule schedule = Schedule.builder()
			.productId(PRODUCT_ID)
			.scheduleDt(LocalDate.of(2026, 5, 1))
			.startTime(LocalTime.of(10, 0))
			.endTime(LocalTime.of(12, 0))
			.status(ReservedStatus.AVAILABLE)
			.capacity(10)
			.build();
		ReflectionTestUtils.setField(schedule, "id", SCHEDULE_ID);

		Product product = Product.builder()
			.sellerId(USER_ID)
			.title("테스트 상품")
			.thumbnailPath("/images/test.jpg")
			.price(BigDecimal.valueOf(35000))
			.maxCapacity(10)
			.description("테스트 설명")
			.status(ProductStatus.ENABLE)
			.roadAddress("서울시 강남구")
			.latitude(BigDecimal.valueOf(37.5))
			.longitude(BigDecimal.valueOf(127.0))
			.build();
		ReflectionTestUtils.setField(product, "id", PRODUCT_ID);

		given(favoriteRepository.findByUserIdAndDeleteDtIsNull(USER_ID)).willReturn(List.of(favorite, favorite2));
		given(scheduleRepository.findAllByIdInAndDeleteDtIsNull(List.of(SCHEDULE_ID, scheduleId2))).willReturn(List.of(schedule));
		given(productRepository.findAllByIdsAndDeleteDtIsNull(List.of(PRODUCT_ID))).willReturn(List.of(product));

		// when
		List<FavoritesResponseDto> result = favoriteService.findByUserIdAndDeleteDtIsNull(USER_ID);

		// then
		assertThat(result).hasSize(1);
		assertThat(result.get(0).id()).isEqualTo(FAVORITE_ID);
		assertThat(result.get(0).productTitle()).isEqualTo("테스트 상품");
	}

	@Test
	void 다른_사용자의_즐겨찾기는_삭제할_수_없다() {
		given(favoriteRepository.findByIdAndUserIdAndDeleteDtIsNull(FAVORITE_ID, USER_ID)).willReturn(null);

		assertThatThrownBy(() -> favoriteService.deleteFavorite(FAVORITE_ID, USER_ID))
			.isInstanceOf(BusinessException.class)
			.hasMessage(CommonErrorCode.NOT_MATCH_USER_LIKE.getMessage());
	}

	@Test
	void 즐겨찾기_생성_요청이_들어오면_컨트롤러가_유스케이스를_호출한다() {
		FavoritesResponseDto response = FavoritesResponseDto.from(favorite);
		given(favoriteUseCase.createFavorite(2, SCHEDULE_ID, USER_ID)).willReturn(response);

		ResponseEntity<ApiResponseDto<FavoritesResponseDto>> result = favoritesRestController.create(2, SCHEDULE_ID, USER_ID);

		assertThat(result.getStatusCode()).isEqualTo(HttpStatus.CREATED);
		assertThat(result.getBody()).isNotNull();
		assertThat(result.getBody().getData()).isEqualTo(response);
		then(favoriteUseCase).should().createFavorite(2, SCHEDULE_ID, USER_ID);
	}

	@Test
	void 즐겨찾기_삭제_요청이_들어오면_컨트롤러가_유스케이스를_호출한다() {
		ResponseEntity<ApiResponseDto<FavoritesResponseDto>> result = favoritesRestController.delete(FAVORITE_ID, USER_ID);

		assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
		then(favoriteUseCase).should().deleteFavorite(FAVORITE_ID, USER_ID);
	}

	@Test
	void 즐겨찾기_목록_조회_요청이_들어오면_컨트롤러가_유스케이스를_호출한다() {
		List<FavoritesResponseDto> response = List.of(FavoritesResponseDto.from(favorite));
		given(favoriteUseCase.findByUserIdAndDeleteDtIsNull(USER_ID)).willReturn(response);

		ResponseEntity<ApiResponseDto<List<FavoritesResponseDto>>> result = favoritesRestController.getList(USER_ID);

		assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(result.getBody()).isNotNull();
		assertThat(result.getBody().getData()).hasSize(1);
		then(favoriteUseCase).should().findByUserIdAndDeleteDtIsNull(USER_ID);
	}
}
