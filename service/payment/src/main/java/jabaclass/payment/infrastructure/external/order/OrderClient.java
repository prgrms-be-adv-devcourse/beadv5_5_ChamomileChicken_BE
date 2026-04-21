package jabaclass.payment.infrastructure.external.order;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import jabaclass.payment.application.port.external.OrderPort;
import jabaclass.payment.common.error.PaymentErrorCode;
import jabaclass.payment.common.error.PaymentException;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class OrderClient implements OrderPort {

	private final RestTemplate restTemplate;

	@Value("${order.service.url}")
	private String baseUrl;

	@Override
	public boolean validateOrder(UUID orderId, int amount) {

		String url = baseUrl + "/api/v1/orders/" + orderId
			+ "/payment-amount/validate?amount=" + amount;

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);

		HttpEntity<Void> request = new HttpEntity<>(headers);

		ResponseEntity<OrderValidationResponse> response =
			restTemplate.exchange(
				url,
				HttpMethod.GET,
				request,
				OrderValidationResponse.class
			);

		if (!response.getStatusCode().is2xxSuccessful()) {
			throw new PaymentException(PaymentErrorCode.INVALID_ORDER_AMOUNT);
		}

		OrderValidationResponse body = response.getBody();
		if (body == null) {
			throw new PaymentException(PaymentErrorCode.ORDER_RESPONSE_INVALID);
		}

		return body.valid();
	}

	private record OrderValidationResponse(boolean valid) {}
}
