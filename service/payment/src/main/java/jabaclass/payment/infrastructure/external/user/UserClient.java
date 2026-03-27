package jabaclass.payment.infrastructure.external.user;

import java.math.BigDecimal;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import jabaclass.payment.application.port.external.UserPort;
import jabaclass.payment.infrastructure.external.user.dto.request.IncreaseDepositRequestDto;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class UserClient implements UserPort {

	private final RestTemplate restTemplate;

	@Value("${user.service.url}")
	private String baseUrl;

	@Override
	public void increaseDeposit(UUID userId, BigDecimal amount, UUID paymentId) {

		String url = baseUrl + "/api/v1/deposits/internal/users/" + userId + "/deposit";

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);

		IncreaseDepositRequestDto body =
			new IncreaseDepositRequestDto(amount, paymentId);

		HttpEntity<IncreaseDepositRequestDto> request =
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
				"유저 예치금 증가 실패. status=" + response.getStatusCode()
			);
		}
	}
}