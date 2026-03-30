package jabaclass.settlement.infrastructure.client.product;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import jabaclass.settlement.application.dto.ProductSettlementDetail;
import jabaclass.settlement.application.port.outt.ProductSettlementPort;
import jabaclass.settlement.infrastructure.config.ClientProperties;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Component
public class ProductClient implements ProductSettlementPort {

	private final RestTemplate restTemplate;
	private final ClientProperties clientProperties;

	@Override
	public List<ProductSettlementDetail> fetchProducts(Set<UUID> productIds) {
		if (productIds == null || productIds.isEmpty()) {
			return List.of();
		}

		ProductBulkRequest request = new ProductBulkRequest(productIds.stream().toList());

		List<ProductItemResponse> responses = restTemplate.exchange(
			clientProperties.product().baseUrl() + "/api/v1/products/bulk",
			HttpMethod.POST,
			new HttpEntity<>(request),
			new ParameterizedTypeReference<List<ProductItemResponse>>() {}
		).getBody();

		if (responses == null) {
			return List.of();
		}

		return responses.stream()
			.map(item -> new ProductSettlementDetail(
				item.productId(),
				item.sellerId(),
				item.productPrice(),
				item.productStatus()
			))
			.toList();
	}

	public record ProductBulkRequest(
		List<UUID> productIds
	) {
	}

	public record ProductItemResponse(
		UUID productId,
		UUID sellerId,
		BigDecimal productPrice,
		String productStatus
	) {
	}
}
