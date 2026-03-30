package jabaclass.product.infrastructure.acl.client;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import jabaclass.product.infrastructure.acl.dto.response.UserResponseDto;
import jabaclass.product.presentation.dto.request.UserBulkReadRequestDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

// 실질적으로 API 통신 하는 곳.
@Component
@RequiredArgsConstructor
@Slf4j
public class SellerApiClient implements SellerClient {

	private final RestTemplate restTemplate;

	@Value("${api.user-host}")
	private String host;

	// seller에 대한 정보 검증 api
	@Override
	public Optional<UserResponseDto> findSeller(UUID sellerId) {
		log.info("SellerApiClient.findSeller :: " + sellerId);

		String url = host + "/api/v1/users/me";

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);

		HttpEntity<UUID> requesst = new HttpEntity<>(sellerId, headers);

		ResponseEntity<UserResponseDto> response =
			restTemplate.exchange(
				url,
				HttpMethod.POST,
				requesst,
				new ParameterizedTypeReference<UserResponseDto>() {
				}
			);

		return Optional.of(response.getBody());
	}

	// 페이지에 보여질 seller 이름 가져오는 api
	@Override
	public Optional<List<UserResponseDto>> findSellerList(List<UUID> sellerIds) {

		String url = host + "/api/v1/users/bulk";

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);

		UserBulkReadRequestDto dto = new UserBulkReadRequestDto(sellerIds);

		HttpEntity<UserBulkReadRequestDto> requesst = new HttpEntity<>(dto, headers);

		ResponseEntity<List<UserResponseDto>> response =
			restTemplate.exchange(
				url,
				HttpMethod.POST,
				requesst,
				new ParameterizedTypeReference<List<UserResponseDto>>() {
				}
			);
		List<UserResponseDto> list = response.getBody();

		return Optional.of(list);
	}
}
