package jabaclass.frontend.client;

import jabaclass.frontend.dto.CreateOrderRequest;
import jabaclass.frontend.dto.CreateOrderResponse;
import jabaclass.frontend.dto.OrderSummaryDto;
import jabaclass.frontend.dto.UpdatePaymentStatusRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class OrderServiceClient {

    private final RestTemplate restTemplate;

    @Value("${services.order-url}")
    private String orderUrl;

    public CreateOrderResponse createOrder(CreateOrderRequest request, String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);

        HttpEntity<CreateOrderRequest> entity = new HttpEntity<>(request, headers);
        ResponseEntity<Map> response = restTemplate.postForEntity(
            orderUrl + "/api/v1/orders",
            entity,
            Map.class
        );

        Map data = (Map) response.getBody();
        return mapToCreateOrderResponse(data);
    }

    public List<OrderSummaryDto> getMyOrders(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        ResponseEntity<List> response = restTemplate.exchange(
            orderUrl + "/api/v1/orders",
            HttpMethod.GET,
            new HttpEntity<>(headers),
            List.class
        );
        List<Map> items = (List<Map>) response.getBody();
        if (items == null) return List.of();
        return items.stream().map(this::mapToOrderSummary).toList();
    }

    private OrderSummaryDto mapToOrderSummary(Map m) {
        OrderSummaryDto dto = new OrderSummaryDto();
        dto.setId(m.get("id") != null ? UUID.fromString(m.get("id").toString()) : null);
        dto.setProductScheduleId(m.get("productScheduleId") != null ? UUID.fromString(m.get("productScheduleId").toString()) : null);
        if (m.get("quantity") != null) dto.setQuantity((int) m.get("quantity"));
        if (m.get("totalAmount") != null) dto.setTotalAmount(new BigDecimal(m.get("totalAmount").toString()));
        if (m.get("status") != null) dto.setStatus(m.get("status").toString());
        return dto;
    }

    public void updatePaymentStatus(UUID orderId, BigDecimal depositAmount, String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);

        UpdatePaymentStatusRequest request = new UpdatePaymentStatusRequest(
            UUID.randomUUID(), "SUCCESS", depositAmount
        );

        HttpEntity<UpdatePaymentStatusRequest> entity = new HttpEntity<>(request, headers);
        restTemplate.put(orderUrl + "/api/v1/orders/" + orderId + "/payment-status", entity);
    }

    private CreateOrderResponse mapToCreateOrderResponse(Map m) {
        CreateOrderResponse dto = new CreateOrderResponse();
        dto.setId(m.get("id") != null ? UUID.fromString(m.get("id").toString()) : null);
        dto.setBuyerId(m.get("buyerId") != null ? UUID.fromString(m.get("buyerId").toString()) : null);
        dto.setProductId(m.get("productId") != null ? UUID.fromString(m.get("productId").toString()) : null);
        dto.setProductScheduleId(m.get("productScheduleId") != null ? UUID.fromString(m.get("productScheduleId").toString()) : null);
        if (m.get("quantity") != null) dto.setQuantity((int) m.get("quantity"));
        if (m.get("totalAmount") != null) dto.setTotalAmount(new BigDecimal(m.get("totalAmount").toString()));
        if (m.get("depositAmount") != null) dto.setDepositAmount(new BigDecimal(m.get("depositAmount").toString()));
        if (m.get("paymentAmount") != null) dto.setPaymentAmount(new BigDecimal(m.get("paymentAmount").toString()));
        if (m.get("status") != null) dto.setStatus(m.get("status").toString());
        return dto;
    }
}
