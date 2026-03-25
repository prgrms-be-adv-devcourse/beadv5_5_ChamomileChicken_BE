package jabaclass.product.infrastructure.acl.client;

import java.util.ArrayList;
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
		log.info(id);

		// 판매자
		if (id.startsWith("1")) {
			return Optional.of(new SellerResponseDto(sellerId, "신짱구", "SELLER"));
		}

		// 일반 사용자
		if (id.startsWith("3")) {
			return Optional.of(new SellerResponseDto(sellerId, "김철수", "USER"));
		}

		// 존재하지 않음
		if (id.startsWith("2")) {
			return Optional.empty();
		}

		return Optional.empty();
	}

	// 페이지에 보여질 seller 이름 가져오는 api
	@Override
	public Optional<List<SellerResponseDto>> findSellerList(List<UUID> sellerIds) {

		Random random = new Random();

		List<SellerResponseDto> sellerResponseDtoList = new ArrayList<>();
		sellerResponseDtoList
			.stream()
			.forEach(sellerResponseDto -> {
				String result = random.ints(2, 0xAC00, 0xD7A3 + 1)
					.mapToObj(i -> String.valueOf((char)i))
					.collect(Collectors.joining());
				sellerResponseDto.form(
					UUID.randomUUID(),
					result,
					SellerRole.SELLER.toString()
				);
			});
		sellerResponseDtoList.add(
			SellerResponseDto.form(
				UUID.fromString("123e4567-e89b-12d3-a456-426614174000"),
				"신짱구",
				SellerRole.SELLER.toString()
			)
		);
		
		return Optional.of(sellerResponseDtoList);
	}
}
