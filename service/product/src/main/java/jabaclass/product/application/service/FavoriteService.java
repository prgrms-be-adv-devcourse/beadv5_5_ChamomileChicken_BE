package jabaclass.product.application.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jabaclass.product.application.exception.BusinessException;
import jabaclass.product.application.usecase.FavoriteUseCase;
import jabaclass.product.common.exception.CommonErrorCode;
import jabaclass.product.domain.model.Favorite;
import jabaclass.product.domain.repository.FavoriteRepository;
import jabaclass.product.presentation.dto.response.FavoritesResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class FavoriteService implements FavoriteUseCase {
	private final FavoriteRepository favoriteRepository;

	@Override
	@Transactional
	public FavoritesResponseDto createFavorite(int quantity, UUID scheduleId, UUID userId) {
		Favorite favorite = Favorite.builder()
			.productScheduleId(scheduleId)
			.userId(userId)
			.quantity(quantity)
			.build();

		Favorite savedFavorite = favoriteRepository.save(favorite);

		return FavoritesResponseDto.from(savedFavorite);
	}

	@Override
	@Transactional
	public void deleteFavorite(UUID favoriteId, UUID userId) {

		// 본인 상품인지 확인
		Favorite matched = favoriteRepository.findByIdAndUserIdAndDeleteDtIsNull(favoriteId, userId);

		if (matched == null) {
			throw new BusinessException(CommonErrorCode.NOT_MATCH_USER_LIKE);
		}

		matched.changeDelete();
	}

	@Override
	public List<FavoritesResponseDto> findByUserIdAndDeleteDtIsNull(UUID userId) {
		// 본인 상품 리스트
		List<Favorite> favorite = favoriteRepository.findByUserIdAndDeleteDtIsNull(userId);

		return favorite.stream()
			.map(FavoritesResponseDto::from)
			.toList();
	}
}
