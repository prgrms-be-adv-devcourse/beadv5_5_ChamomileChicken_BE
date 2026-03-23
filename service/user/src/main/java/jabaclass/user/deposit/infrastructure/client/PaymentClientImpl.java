package jabaclass.user.deposit.infrastructure.client;

import java.math.BigDecimal;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import jabaclass.user.deposit.infrastructure.client.dto.ConfirmRequestDto;
import jabaclass.user.deposit.infrastructure.client.dto.ConfirmResponseDto;
import jabaclass.user.deposit.infrastructure.client.dto.PrepareRequestDto;
import jabaclass.user.deposit.infrastructure.client.dto.PrepareResponseDto;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class PaymentClientImpl implements PaymentClient {

	private final RestTemplate restTemplate;

	@Value("${payment.service.url}")
	private String paymentServiceUrl;

	@Override
	public UUID createPayment(UUID userId, BigDecimal amount, String paymentMethod) {
		UUID prepareId = prepareDepositPayment(userId, amount, paymentMethod);
		return confirmDepositPayment(prepareId);
	}

	@Override
	public UUID prepareDepositPayment(UUID userId, BigDecimal amount, String paymentMethod) {
		String url = paymentServiceUrl + "/api/v1/payments/deposit/prepare";

		PrepareRequestDto request = new PrepareRequestDto(userId, amount, paymentMethod);

		ResponseEntity<PrepareResponseDto> response = restTemplate.postForEntity(
			url,
			request,
			PrepareResponseDto.class
		);

		return response.getBody().prepareId();
	}

	@Override
	public UUID confirmDepositPayment(UUID prepareId) {
		String url = paymentServiceUrl + "/api/v1/payments/deposit/comfirm";

		ConfirmRequestDto request = new ConfirmRequestDto(prepareId);

		ResponseEntity<ConfirmResponseDto> response = restTemplate.postForEntity(
			url,
			request,
			ConfirmResponseDto.class
		);

		return response.getBody().paymentId();
	}
}