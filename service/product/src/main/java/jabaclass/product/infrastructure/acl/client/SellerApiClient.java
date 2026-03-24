package jabaclass.product.infrastructure.acl.client;

import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Component;

import jabaclass.product.infrastructure.acl.dto.SellerResposeDto;

// 실질적으로 API 통신 하는 곳.
@Component
public class SellerApiClient implements SellerClient {

	// TODO UserAPI를 통해 seller 정보를 받아오는 작업 필요
	@Override
	public Optional<SellerResposeDto> findSeller(UUID sellerId) {

		String id = sellerId.toString();

		// 판매자
		if (id.startsWith("1")) {
			return Optional.of(new SellerResposeDto(sellerId, "신짱구", "SELLER"));
		}

		// 일반 사용자
		if (id.startsWith("3")) {
			return Optional.of(new SellerResposeDto(sellerId, "김철수", "USER"));
		}

		// 존재하지 않음
		if (id.startsWith("2")) {
			return Optional.empty();
		}

		return Optional.empty();
	}
}
