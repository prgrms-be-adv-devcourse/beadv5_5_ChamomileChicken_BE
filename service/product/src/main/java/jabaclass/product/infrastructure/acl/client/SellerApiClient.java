package jabaclass.product.infrastructure.acl.client;

import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import jabaclass.product.infrastructure.acl.dto.SellerResponseDto;
import jabaclass.product.infrastructure.acl.dto.SellerRole;
import lombok.extern.slf4j.Slf4j;

// 실질적으로 API 통신 하는 곳.
@Component
@Slf4j
public class SellerApiClient implements SellerClient {

	// TODO UserAPI를 통해 seller 정보를 받아오는 작업 필요
	// seller에 대한 정보 검증 api
	@Override
	public Optional<SellerResponseDto> findSeller(UUID sellerId) {

		String id = sellerId.toString();

		// 판매자
		if (id.startsWith("1")) {
			return Optional.of(new SellerResponseDto(sellerId, "신짱구", "SELLER"));
		}

		// 일반 사용자
		//	if (id.startsWith("3")) {
		//		return Optional.of(new SellerResponseDto(sellerId, "김철수", "USER"));
		//	}

		// 존재하지 않음
		//	if (id.startsWith("2")) {
		//		return Optional.empty();
		//	}

		return Optional.of(new SellerResponseDto(sellerId, "김철수", "USER"));
	}

	// 페이지에 보여질 seller 이름 가져오는 api
	@Override
	public Optional<List<SellerResponseDto>> findSellerList(List<UUID> sellerIds) {

		Random random = new Random();

		List<SellerResponseDto> sellerResponseDtoList = sellerIds.stream()
			.map(id -> new SellerResponseDto(id, "판매자_" + id.toString().substring(0, 4), SellerRole.SELLER.toString()))
			.collect(Collectors.toList());

		return Optional.of(sellerResponseDtoList);
	}
}
