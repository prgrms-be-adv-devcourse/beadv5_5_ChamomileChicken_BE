package jabaclass.product;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
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
import org.springframework.test.util.ReflectionTestUtils;

import jabaclass.product.application.exception.BusinessException;
import jabaclass.product.application.service.FavoriteService;
import jabaclass.product.application.usecase.FavoriteUseCase;
import jabaclass.product.common.exception.ApiResponseDto;
import jabaclass.product.common.exception.CommonErrorCode;
import jabaclass.product.domain.model.Favorite;
import jabaclass.product.domain.repository.FavoriteRepository;
import jabaclass.product.presentation.controller.FavoritesRestController;
import jabaclass.product.presentation.dto.respose.FavoritesResposeDto;

@ExtendWith(MockitoExtension.class)
class FavoriteTest {

	@InjectMocks
	private FavoriteService favoriteService;

	@InjectMocks
	private FavoritesRestController favoritesRestController;

	@Mock
	private FavoriteRepository favoriteRepository;

	@Mock
	private FavoriteUseCase favoriteUseCase;

	private static final UUID USER_ID = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
	private static final UUID SCHEDULE_ID = UUID.fromString("223e4567-e89b-12d3-a456-426614174000");
	private static final UUID FAVORITE_ID = UUID.fromString("323e4567-e89b-12d3-a456-426614174000");

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

		FavoritesResposeDto result = favoriteService.createFavorite(2, SCHEDULE_ID, USER_ID);

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
	void 내_즐겨찾기_목록을_조회한다() {
		given(favoriteRepository.findByUserIdAndDeleteDtIsNull(USER_ID)).willReturn(List.of(favorite));

		List<FavoritesResposeDto> result = favoriteService.findByUserIdAndDeleteDtIsNull(USER_ID);

		assertThat(result).hasSize(1);
		assertThat(result.get(0).id()).isEqualTo(FAVORITE_ID);
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
		FavoritesResposeDto response = new FavoritesResposeDto(FAVORITE_ID, SCHEDULE_ID, 2);
		given(favoriteUseCase.createFavorite(2, SCHEDULE_ID, USER_ID)).willReturn(response);

		ResponseEntity<ApiResponseDto<FavoritesResposeDto>> result = favoritesRestController.create(2, SCHEDULE_ID, USER_ID);

		assertThat(result.getStatusCode()).isEqualTo(HttpStatus.CREATED);
		assertThat(result.getBody()).isNotNull();
		assertThat(result.getBody().getData()).isEqualTo(response);
		then(favoriteUseCase).should().createFavorite(2, SCHEDULE_ID, USER_ID);
	}

	@Test
	void 즐겨찾기_삭제_요청이_들어오면_컨트롤러가_유스케이스를_호출한다() {
		ResponseEntity<ApiResponseDto<FavoritesResposeDto>> result = favoritesRestController.delete(FAVORITE_ID, USER_ID);

		assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
		then(favoriteUseCase).should().deleteFavorite(FAVORITE_ID, USER_ID);
	}

	@Test
	void 즐겨찾기_목록_조회_요청이_들어오면_컨트롤러가_유스케이스를_호출한다() {
		List<FavoritesResposeDto> response = List.of(new FavoritesResposeDto(FAVORITE_ID, SCHEDULE_ID, 2));
		given(favoriteUseCase.findByUserIdAndDeleteDtIsNull(USER_ID)).willReturn(response);

		ResponseEntity<ApiResponseDto<List<FavoritesResposeDto>>> result = favoritesRestController.getList(USER_ID);

		assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(result.getBody()).isNotNull();
		assertThat(result.getBody().getData()).hasSize(1);
		then(favoriteUseCase).should().findByUserIdAndDeleteDtIsNull(USER_ID);
	}
}
