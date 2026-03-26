package jabaclass.payment.infrastructure.external.order;

import java.math.BigDecimal;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import jabaclass.payment.application.port.external.OrderPort;
import jabaclass.payment.domain.model.PaymentResultStatus;
import jabaclass.payment.infrastructure.external.order.dto.request.OrderStatusUpdateRequestDto;
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
			throw new IllegalStateException("주문 금액이 일치하지 않습니다.");
		}

		OrderValidationResponse body = response.getBody();
		if (body == null) {
			throw new IllegalStateException("Order 검증 응답이 비어있습니다.");
		}

		return body.valid();
	}

	@Override
	public void updatePaymentStatus(UUID orderId, UUID paymentId, int depositAmount, String status) {

		String url = baseUrl + "/api/v1/orders/" + orderId + "/payment-status";

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);

		PaymentResultStatus paymentStatus =
			status.equals("PAID")
				? PaymentResultStatus.SUCCESS
				: PaymentResultStatus.FAILED;

		OrderStatusUpdateRequestDto body =
			new OrderStatusUpdateRequestDto(
				paymentId,
				paymentStatus,
				BigDecimal.valueOf(depositAmount)
			);

		HttpEntity<OrderStatusUpdateRequestDto> request =
			new HttpEntity<>(body, headers);

		ResponseEntity<Void> response =
			restTemplate.exchange(
				url,
				HttpMethod.PUT,
				request,
				Void.class
			);

		if (!response.getStatusCode().is2xxSuccessful()) {
			throw new IllegalStateException(
				"Order 상태 업데이트 실패. status=" + response.getStatusCode()
			);
		}
	}

	private record OrderValidationResponse(boolean valid) {}
}