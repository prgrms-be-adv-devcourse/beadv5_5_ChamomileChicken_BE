package jabaclass.payment.infrastructure.external.toss;

import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import jabaclass.payment.application.port.external.PaymentGatewayPort;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class TossPaymentClient implements PaymentGatewayPort {

	private final RestTemplate restTemplate;

	@Value("${toss.secret-key}")
	private String secretKey;

	@Override
	public void confirm(String paymentKey, String orderId, int amount) {

		String url = "https://api.tosspayments.com/v1/payments/confirm";

		HttpHeaders headers = new HttpHeaders();
		headers.setBasicAuth(secretKey, "");
		headers.setContentType(MediaType.APPLICATION_JSON);

		Map<String, Object> body = Map.of(
			"paymentKey", paymentKey,
			"orderId", orderId,
			"amount", amount
		);

		HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

		try {
			restTemplate.postForEntity(url, request, String.class);
		} catch (Exception e) {
			throw new RuntimeException("결제 승인 실패", e);
		}
	}
}
