package jabaclass.order.order.infrastructure.client.product;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import jabaclass.order.order.application.client.ProductClient;
import jabaclass.order.order.infrastructure.client.product.dto.ProductReservationReleaseRequestDto;
import jabaclass.order.order.infrastructure.client.product.dto.ProductReservationRequestDto;
import jabaclass.order.order.infrastructure.client.product.dto.ProductReservationResponseDto;
import jabaclass.order.order.infrastructure.client.product.dto.ProductReservationStatus;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ProductClientAdapter implements ProductClient {

	private final RestTemplate restTemplate;

	@Value("${external.products.base-url:http://localhost:9004}")
	private String productBaseUrl;

	@Override
	public ProductReservationResponseDto reserve(UUID productScheduleId, UUID userId, Integer quantity) {
		return restTemplate.postForObject(
			productBaseUrl + "/api/v1/products/reservations",
			new ProductReservationRequestDto(productScheduleId, userId, ProductReservationStatus.PENDING, quantity),
			ProductReservationResponseDto.class
		);
	}

	@Override
	public void updateReservation(UUID productScheduleId, UUID userId, ProductReservationStatus status,
		UUID productUserId,
		Integer quantity) {
		restTemplate.postForLocation(
			productBaseUrl + "/api/v1/products/reservations/status",
			new ProductReservationReleaseRequestDto(productScheduleId, userId, status, productUserId, quantity)
		);
	}
}
