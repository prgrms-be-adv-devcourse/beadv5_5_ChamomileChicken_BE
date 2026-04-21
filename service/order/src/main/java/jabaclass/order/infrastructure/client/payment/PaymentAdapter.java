package jabaclass.order.infrastructure.client.payment;

import java.math.BigDecimal;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import jabaclass.order.application.exception.OrderErrorCode;
import jabaclass.order.application.port.external.PaymentPort;
import jabaclass.order.common.error.BusinessException;
import jabaclass.order.infrastructure.client.payment.dto.InternalRefundRequestDto;
import jabaclass.order.infrastructure.client.payment.dto.PaymentRefundInfoResponseDto;
import jabaclass.order.infrastructure.client.payment.dto.InternalRefundResponseDto;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class PaymentAdapter implements PaymentPort {

	private final RestTemplate restTemplate;

	@Value("${external.payments.base-url:http://localhost:9001}")
	private String paymentBaseUrl;

	@Override
	public BigDecimal refund(UUID orderId, BigDecimal refundRate) {
		InternalRefundResponseDto response = restTemplate.postForObject(
			paymentBaseUrl + "/api/v1/payments/internal/refunds",
			new InternalRefundRequestDto(orderId, refundRate),
			InternalRefundResponseDto.class
		);
		if (response == null) {
			throw new BusinessException(OrderErrorCode.EXTERNAL_PAYMENT_ERROR);
		}
		return response.depositRefundAmount();
	}

	@Override
	public PaymentRefundInfoResponseDto getRefundInfo(UUID orderId) {
		PaymentRefundInfoResponseDto response = restTemplate.getForObject(
			paymentBaseUrl + "/api/v1/payments/internal/refunds/orders/" + orderId,
			PaymentRefundInfoResponseDto.class
		);
		if (response == null) {
			throw new BusinessException(OrderErrorCode.EXTERNAL_PAYMENT_ERROR);
		}
		return response;
	}
}
