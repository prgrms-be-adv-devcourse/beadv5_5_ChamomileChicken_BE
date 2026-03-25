package jabaclass.payment.infrastructure.external.order;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import jabaclass.payment.application.port.out.OrderPort;
import jabaclass.payment.infrastructure.external.order.dto.request.OrderStatusUpdateRequestDto;
import jabaclass.payment.infrastructure.external.order.dto.request.OrderValidationRequestDto;
import jabaclass.payment.infrastructure.external.order.dto.response.OrderValidationResponseDto;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class OrderClient implements OrderPort {

	private final RestTemplate restTemplate;

	@Value("${order.service.url}")
	private String baseUrl;

	/*@Value("${internal.token}")
	private String internalToken;*/

	// 주문 금액 검증
	@Override
	public boolean validateOrder(UUID orderId, int amount) {

		String url = baseUrl + "/api/v1/orders/" + orderId + "/payment-amount/validate";

		HttpHeaders headers = new HttpHeaders();
		// headers.set("X-Internal-Token", internalToken);
		headers.setContentType(MediaType.APPLICATION_JSON);

		OrderValidationRequestDto body =
			new OrderValidationRequestDto(amount);

		HttpEntity<OrderValidationRequestDto> request =
			new HttpEntity<>(body, headers);

		ResponseEntity<OrderValidationResponseDto> response =
			restTemplate.exchange(
				url,
				HttpMethod.POST,
				request,
				OrderValidationResponseDto.class
			);

		// TODO: 상태코드 체크
		// TODO: null 체크

		return response.getBody().available();
	}

	// 결제 결과 통지
	@Override
	public void updatePaymentStatus(UUID orderId, UUID paymentId, String status) {

		String url = baseUrl + "/api/v1/orders/" + orderId + "/payment-status";

		HttpHeaders headers = new HttpHeaders();
		// headers.set("X-Internal-Token", internalToken);
		headers.setContentType(MediaType.APPLICATION_JSON);

		OrderStatusUpdateRequestDto body =
			new OrderStatusUpdateRequestDto(paymentId, status);

		HttpEntity<OrderStatusUpdateRequestDto> request =
			new HttpEntity<>(body, headers);

		restTemplate.exchange(
			url,
			HttpMethod.PATCH,
			request,
			Void.class
		);

		// TODO: 실패 시 재시도 처리
	}
}