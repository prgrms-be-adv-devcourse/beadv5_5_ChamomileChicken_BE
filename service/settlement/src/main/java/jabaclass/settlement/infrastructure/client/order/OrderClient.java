package jabaclass.settlement.infrastructure.client.order;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import jabaclass.settlement.application.dto.OrderSettlementDetail;
import jabaclass.settlement.application.port.external.OrderSettlementPort;
import jabaclass.settlement.infrastructure.config.ClientProperties;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Component
public class OrderClient implements OrderSettlementPort {

	private final RestTemplate restTemplate;
	private final ClientProperties clientProperties;

	@Override
	public List<OrderSettlementDetail> fetchOrders(Set<UUID> orderIds) {
		if (orderIds == null || orderIds.isEmpty()) {
			return List.of();
		}

		OrderBulkRequest request = new OrderBulkRequest(orderIds.stream().toList());

		List<OrderItemResponse> responses = restTemplate.exchange(
			clientProperties.order().baseUrl() + "/api/v1/orders/bulk",
			HttpMethod.POST,
			new HttpEntity<>(request),
			new ParameterizedTypeReference<List<OrderItemResponse>>() {}
		).getBody();

		if (responses == null) {
			return List.of();
		}

		return responses.stream()
			.map(item -> new OrderSettlementDetail(
				item.orderId(),
				item.productScheduleId(),
				item.buyerId(),
				item.participantUserId(),
				item.quantity(),
				item.unitPrice(),
				item.orderPrice(),
				item.orderStatus()
			))
			.toList();
	}

	public record OrderBulkRequest(
		List<UUID> orderIds
	) {
	}

	public record OrderItemResponse(
		UUID orderId,
		UUID productScheduleId,
		UUID buyerId,
		UUID participantUserId,
		Integer quantity,
		BigDecimal unitPrice,
		BigDecimal orderPrice,
		String orderStatus
	) {
	}
}
