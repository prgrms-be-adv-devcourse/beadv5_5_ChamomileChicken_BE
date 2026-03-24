package jabaclass.user.deposit.infrastructure.client;

import java.math.BigDecimal;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import jabaclass.user.common.error.BusinessException;
import jabaclass.user.deposit.domain.error.DepositErrorCode;
import jabaclass.user.deposit.infrastructure.client.dto.DepositConfirmRequestDto;
import jabaclass.user.deposit.infrastructure.client.dto.DepositConfirmResponseDto;
import jabaclass.user.deposit.infrastructure.client.dto.DepositPrepareRequestDto;
import jabaclass.user.deposit.infrastructure.client.dto.DepositPrepareResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentClientImpl implements PaymentClient {

	private final RestTemplate restTemplate;

	@Value("${payment.service.url}")
	private String paymentServiceUrl;

	@Override
	public UUID createPayment(UUID userId, BigDecimal amount, String paymentMethod) {
		UUID depositPaymentsId = prepareDepositPayment(userId, amount, paymentMethod);
		boolean isPaid = confirmDepositPayment(depositPaymentsId);

		if (!isPaid) {
			throw new BusinessException(DepositErrorCode.PAYMENT_FAILED);
		}

		return depositPaymentsId;
	}

	@Override
	public UUID prepareDepositPayment(UUID userId, BigDecimal amount, String paymentMethod) {
		try {
			String url = paymentServiceUrl + "/api/v1/payments/deposits/prepare";

			DepositPrepareRequestDto request = new DepositPrepareRequestDto(userId, amount, paymentMethod);

			ResponseEntity<DepositPrepareResponseDto> response = restTemplate.postForEntity(
				url, request, DepositPrepareResponseDto.class
			);

			return response.getBody().depositPaymentsId();

		} catch (RestClientException e) {
			log.error("결제 준비 실패 userId={}", userId, e);
			throw new BusinessException(DepositErrorCode.PAYMENT_SERVICE_UNAVAILABLE);
		}
	}

	@Override
	public boolean confirmDepositPayment(UUID prepareId) {
		try {
			String url = paymentServiceUrl + "/api/v1/payments/deposits/confirm";

			DepositConfirmRequestDto request = new DepositConfirmRequestDto(prepareId);

			ResponseEntity<DepositConfirmResponseDto> response = restTemplate.postForEntity(
				url, request, DepositConfirmResponseDto.class
			);

			return response.getBody().isPaid();

		} catch (RestClientException e) {
			log.error("결제 승인 실패 prepareId={}", prepareId, e);
			throw new BusinessException(DepositErrorCode.PAYMENT_SERVICE_UNAVAILABLE);
		}
	}
}