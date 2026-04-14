package jabaclass.product;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import jabaclass.product.application.usecase.FavoriteUseCase;
import jabaclass.product.common.exception.ApiResponseDto;
import jabaclass.product.presentation.controller.FavoritesRestController;
import jabaclass.product.presentation.dto.respose.FavoritesResposeDto;

@ExtendWith(MockitoExtension.class)
class FavoritesRestControllerTest {

	@InjectMocks
	private FavoritesRestController favoritesRestController;

	@Mock
	private FavoriteUseCase favoriteUseCase;

	private static final UUID SCHEDULE_ID = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
	private static final UUID FAVORITE_ID = UUID.fromString("223e4567-e89b-12d3-a456-426614174000");
	private static final UUID USER_ID = UUID.fromString("323e4567-e89b-12d3-a456-426614174000");

	@Test
	void 즐겨찾기_생성_요청이_들어오면_유스케이스를_호출한다() {
		FavoritesResposeDto response = new FavoritesResposeDto(FAVORITE_ID, SCHEDULE_ID, 2);
		given(favoriteUseCase.createFavorite(2, SCHEDULE_ID, USER_ID)).willReturn(response);

		ResponseEntity<ApiResponseDto<FavoritesResposeDto>> result = favoritesRestController.create(2, SCHEDULE_ID, USER_ID);

		assertThat(result.getStatusCode()).isEqualTo(HttpStatus.CREATED);
		assertThat(result.getBody()).isNotNull();
		assertThat(result.getBody().getData()).isEqualTo(response);
		then(favoriteUseCase).should().createFavorite(2, SCHEDULE_ID, USER_ID);
	}

	@Test
	void 즐겨찾기_삭제_요청이_들어오면_유스케이스를_호출한다() {
		ResponseEntity<ApiResponseDto<FavoritesResposeDto>> result = favoritesRestController.delete(FAVORITE_ID, USER_ID);

		assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
		then(favoriteUseCase).should().deleteFavorite(FAVORITE_ID, USER_ID);
	}

	@Test
	void 즐겨찾기_목록_조회_요청이_들어오면_유스케이스를_호출한다() {
		List<FavoritesResposeDto> response = List.of(new FavoritesResposeDto(FAVORITE_ID, SCHEDULE_ID, 2));
		given(favoriteUseCase.findByUserIdAndDeleteDtIsNull(USER_ID)).willReturn(response);

		ResponseEntity<ApiResponseDto<List<FavoritesResposeDto>>> result = favoritesRestController.getList(USER_ID);

		assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(result.getBody()).isNotNull();
		assertThat(result.getBody().getData()).hasSize(1);
		then(favoriteUseCase).should().findByUserIdAndDeleteDtIsNull(USER_ID);
	}
}
