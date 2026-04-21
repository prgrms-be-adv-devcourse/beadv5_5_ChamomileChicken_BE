package jabaclass.order.infrastructure.client.product;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import jabaclass.order.application.port.external.ProductPort;
import jabaclass.order.infrastructure.client.product.dto.ProductReservationRequestDto;
import jabaclass.order.infrastructure.client.product.dto.ProductReservationResponseDto;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ProductAdapter implements ProductPort {

	private final RestTemplate restTemplate;
	private final ObjectMapper objectMapper;

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
		String response = restTemplate.getForObject(
			productBaseUrl + "/api/v1/products/schedules/" + productScheduleId + "/start-date",
			String.class
		);
		if (response == null) {
			throw new RuntimeException("스케줄 날짜 조회 실패: " + productScheduleId);
		}
		try {
			return LocalDate.parse(objectMapper.readTree(response).get("startDate").asText());
		} catch (Exception e) {
			throw new RuntimeException("스케줄 날짜 파싱 실패: " + productScheduleId, e);
		}
	}
}
