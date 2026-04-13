package jabaclass.order.infrastructure.client.product;

import java.math.BigDecimal;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import jabaclass.order.application.port.external.ProductPort;
import jabaclass.order.infrastructure.client.product.dto.ProductReservationRequestDto;
import jabaclass.order.infrastructure.client.product.dto.ProductReservationResponseDto;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ProductAdapter implements ProductPort {

	private final RestTemplate restTemplate;

	@Value("${external.products.base-url:http://localhost:9004}")
	private String productBaseUrl;

	@Override
	public ProductReservationResponseDto reserve(UUID productScheduleId, UUID userId, Integer quantity,
		BigDecimal price) {
		return restTemplate.postForObject(
			productBaseUrl + "/api/v1/products/reservations",
			new ProductReservationRequestDto(
				productScheduleId,
				userId,
				quantity,
				price
			),
			ProductReservationResponseDto.class
		);
	}
}
