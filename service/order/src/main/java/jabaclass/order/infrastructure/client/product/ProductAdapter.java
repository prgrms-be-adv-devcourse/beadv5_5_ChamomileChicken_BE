package jabaclass.order.infrastructure.client.product;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import jabaclass.order.application.exception.OrderErrorCode;
import jabaclass.order.application.port.external.ProductPort;
import jabaclass.order.common.error.BusinessException;
import jabaclass.order.infrastructure.client.product.dto.ProductReservationRequestDto;
import jabaclass.order.infrastructure.client.product.dto.ProductReservationResponseDto;
import jabaclass.order.infrastructure.client.product.dto.ProductScheduleDateResponseDto;
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

	@Override
	public LocalDate getScheduleStartDate(UUID productScheduleId) {
		ProductScheduleDateResponseDto response = restTemplate.getForObject(
			productBaseUrl + "/api/v1/products/schedules/" + productScheduleId + "/start-date",
			ProductScheduleDateResponseDto.class
		);
		if (response == null) {
			throw new BusinessException(OrderErrorCode.EXTERNAL_PRODUCT_ERROR);
		}
		return response.startDate();
	}
}
