package jabaclass.frontend.client;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import jabaclass.frontend.dto.CreateProductRequest;
import jabaclass.frontend.dto.CreateScheduleRequest;
import jabaclass.frontend.dto.ProductDto;
import jabaclass.frontend.dto.ScheduleDto;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ProductServiceClient {

	private final RestTemplate restTemplate;

	@Value("${services.product-url}")
	private String productUrl;

	public UUID createProduct(CreateProductRequest request, String accessToken) {
		HttpHeaders headers = new HttpHeaders();
		headers.setBearerAuth(accessToken);
		HttpEntity<CreateProductRequest> entity = new HttpEntity<>(request, headers);
		Map response = restTemplate.postForObject(productUrl + "/api/v1/products", entity, Map.class);
		Map data = (Map)response.get("data");
		return UUID.fromString(data.get("id").toString());
	}

	public void createSchedule(UUID productId, CreateScheduleRequest request, String accessToken) {
		HttpHeaders headers = new HttpHeaders();
		headers.setBearerAuth(accessToken);
		HttpEntity<CreateScheduleRequest> entity = new HttpEntity<>(request, headers);
		restTemplate.postForObject(productUrl + "/api/v1/products/" + productId + "/schedules", entity, Map.class);
	}

	public List<ProductDto> getProducts() {
		Map response = restTemplate.getForObject(
			productUrl + "/api/v1/products?thisPage=0&pageSize=20&status=ENABLE",
			Map.class
		);
		// SearchProductResponseDto.content 추출
		Map data = (Map)response.get("data");
		List<Map> content = (List<Map>)data.get("content");
		return content.stream().map(this::mapToProductDto).toList();
	}

	public ProductDto getProduct(UUID productId) {
		Map response = restTemplate.getForObject(
			productUrl + "/api/v1/products/" + productId,
			Map.class
		);
		Map data = (Map)response.get("data");
		return mapToProductDto(data);
	}

	public List<ScheduleDto> getSchedules(UUID productId) {
		Map response = restTemplate.getForObject(
			productUrl + "/api/v1/products/" + productId + "/schedules",
			Map.class
		);
		List<Map> data = (List<Map>)response.get("data");
		return data.stream().map(this::mapToScheduleDto).toList();
	}

	public Map<UUID, ProductDto> getProductsByScheduleIds(Set<UUID> scheduleIds) {
		Map<UUID, ProductDto> productsByScheduleId = new HashMap<>();

		if (scheduleIds == null || scheduleIds.isEmpty()) {
			return productsByScheduleId;
		}

		for (ProductDto product : getProducts()) {
			List<ScheduleDto> schedules = getSchedules(product.getId());
			for (ScheduleDto schedule : schedules) {
				if (scheduleIds.contains(schedule.getId())) {
					productsByScheduleId.put(schedule.getId(), product);
				}
			}

			if (productsByScheduleId.size() == scheduleIds.size()) {
				break;
			}
		}

		return productsByScheduleId;
	}

	private ProductDto mapToProductDto(Map m) {
		ProductDto dto = new ProductDto();
		dto.setId(m.get("id") != null ? UUID.fromString(m.get("id").toString()) : null);
		dto.setSellerName((String)m.get("sellerName"));
		dto.setTitle((String)m.get("title"));
		dto.setDescription((String)m.get("description"));
		dto.setDescriptionImage((String)m.get("descriptionImage"));
		dto.setStatusName((String)m.get("statusName"));
		if (m.get("maxCapacity") != null)
			dto.setMaxCapacity((int)m.get("maxCapacity"));
		if (m.get("price") != null)
			dto.setPrice(new java.math.BigDecimal(m.get("price").toString()));
		return dto;
	}

	private ScheduleDto mapToScheduleDto(Map m) {
		ScheduleDto dto = new ScheduleDto();
		dto.setId(m.get("id") != null ? UUID.fromString(m.get("id").toString()) : null);
		dto.setProductId(m.get("productId") != null ? UUID.fromString(m.get("productId").toString()) : null);
		dto.setScheduleDt(
			m.get("scheduleDt") != null ? java.time.LocalDate.parse(m.get("scheduleDt").toString()) : null);
		dto.setStartTime(m.get("startTime") != null ? java.time.LocalTime.parse(m.get("startTime").toString()) : null);
		dto.setEndTime(m.get("endTime") != null ? java.time.LocalTime.parse(m.get("endTime").toString()) : null);
		dto.setStatus((String)m.get("status"));
		if (m.get("maxCapacity") != null)
			dto.setMaxCapacity((int)m.get("maxCapacity"));
		return dto;
	}
}
