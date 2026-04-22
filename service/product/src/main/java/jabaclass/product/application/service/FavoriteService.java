package jabaclass.product.application.service;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jabaclass.product.application.exception.BusinessException;
import jabaclass.product.application.usecase.FavoriteUseCase;
import jabaclass.product.common.exception.CommonErrorCode;
import jabaclass.product.domain.model.Favorite;
import jabaclass.product.domain.model.Product;
import jabaclass.product.domain.model.Schedule;
import jabaclass.product.domain.repository.FavoriteRepository;
import jabaclass.product.domain.repository.ProductRepository;
import jabaclass.product.domain.repository.ScheduleRepository;
import jabaclass.product.presentation.dto.response.FavoritesResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class FavoriteService implements FavoriteUseCase {
	private final FavoriteRepository favoriteRepository;
	private final ScheduleRepository scheduleRepository;
	private final ProductRepository productRepository;

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
		List<Favorite> favorites = favoriteRepository.findByUserIdAndDeleteDtIsNull(userId);

		if (favorites.isEmpty()) {
			return List.of();
		}

		// 1. 찜 목록의 스케줄 ID 추출 후 일괄 조회
		List<UUID> scheduleIds = favorites.stream()
			.map(Favorite::getProductScheduleId)
			.toList();
		Map<UUID, Schedule> scheduleMap = scheduleRepository.findAllByIdInAndDeleteDtIsNull(scheduleIds)
			.stream()
			.collect(Collectors.toMap(Schedule::getId, s -> s));

		// 2. 유효한 스케줄의 상품 ID 추출 후 일괄 조회
		List<UUID> productIds = scheduleMap.values().stream()
			.map(Schedule::getProductId)
			.distinct()
			.toList();
		Map<UUID, Product> productMap = productRepository.findAllByIdsAndDeleteDtIsNull(productIds)
			.stream()
			.collect(Collectors.toMap(Product::getId, p -> p));

		// 3. 유효한 찜 항목만 응답 생성
		return favorites.stream()
			.filter(f -> {
				if (!scheduleMap.containsKey(f.getProductScheduleId())) {
					log.warn("찜 항목의 일정을 찾을 수 없습니다. favoriteId={}, scheduleId={}",
						f.getId(), f.getProductScheduleId());
					return false;
				}
				Schedule schedule = scheduleMap.get(f.getProductScheduleId());
				if (!productMap.containsKey(schedule.getProductId())) {
					log.warn("찜 항목의 상품을 찾을 수 없습니다. favoriteId={}, productId={}",
						f.getId(), schedule.getProductId());
					return false;
				}
				return true;
			})
			.map(f -> {
				Schedule schedule = scheduleMap.get(f.getProductScheduleId());
				Product product = productMap.get(schedule.getProductId());
				return FavoritesResponseDto.from(f, schedule, product);
			})
			.toList();
	}
}