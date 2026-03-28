package jabaclass.frontend.client;

import jabaclass.frontend.dto.PreparePaymentRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class PaymentServiceClient {

    private final RestTemplate restTemplate;

    @Value("${services.payment-url}")
    private String paymentUrl;

    public void preparePayment(PreparePaymentRequest request, String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<PreparePaymentRequest> entity = new HttpEntity<>(request, headers);
        restTemplate.postForEntity(paymentUrl + "/api/v1/payments/prepare", entity, Map.class);
    }

    public Map confirmPayment(UUID orderId, String paymentKey, int amount, String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> body = Map.of(
            "orderId", orderId,
            "paymentKey", paymentKey,
            "amount", amount
        );

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
        ResponseEntity<Map> response = restTemplate.postForEntity(
            paymentUrl + "/api/v1/payments/confirm",
            entity,
            Map.class
        );
        return response.getBody();
    }

    public UUID prepareDepositPayment(UUID userId, int amount, String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> body = Map.of(
            "userId", userId,
            "amount", new java.math.BigDecimal(amount),
            "paymentMethod", "CARD"
        );

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
        ResponseEntity<Map> response = restTemplate.postForEntity(
            paymentUrl + "/api/v1/payments/deposits/prepare",
            entity,
            Map.class
        );
        return UUID.fromString(response.getBody().get("depositPaymentsId").toString());
    }

    public void confirmDepositPayment(UUID depositPaymentsId, String paymentKey, int amount, String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> body = Map.of(
            "depositPaymentsId", depositPaymentsId,
            "paymentKey", paymentKey,
            "amount", amount
        );

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
        restTemplate.postForEntity(
            paymentUrl + "/api/v1/payments/deposits/confirm",
            entity,
            Map.class
        );
    }
}