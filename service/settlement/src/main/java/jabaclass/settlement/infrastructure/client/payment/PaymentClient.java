package jabaclass.settlement.infrastructure.client.payment;

import java.math.BigDecimal;
import java.net.URI;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.web.util.UriComponentsBuilder;

import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import jabaclass.settlement.application.dto.PaymentSettlementSource;
import jabaclass.settlement.application.dto.RefundSettlementSource;
import jabaclass.settlement.application.dto.SettlementSliceResult;
import jabaclass.settlement.application.port.outt.PaymentSettlementPort;
import jabaclass.settlement.infrastructure.config.ClientProperties;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Component
public class PaymentClient implements PaymentSettlementPort {

	private final RestTemplate restTemplate;
	private final ClientProperties clientProperties;

	@Override
	public SettlementSliceResult<PaymentSettlementSource> fetchPayments(
		LocalDateTime from,
		LocalDateTime to,
		LocalDateTime cursorUpdatedAt,
		UUID cursorId,
		int size
	) {
		URI uri = UriComponentsBuilder
			.fromUriString(clientProperties.payment().baseUrl())
			.path("/api/v1/payments/settlement-targets")
			.queryParam("from", from)
			.queryParam("to", to)
			.queryParam("cursorUpdatedAt", cursorUpdatedAt)
			.queryParam("cursorId", cursorId)
			.queryParam("size", size)
			.build()
			.toUri();

		PaymentSliceResponse response = restTemplate.exchange(
			uri,
			HttpMethod.GET,
			null,
			PaymentSliceResponse.class
		).getBody();

		if (response == null) {
			return new SettlementSliceResult<>(List.of(), false, null, null);
		}

		List<PaymentSettlementSource> content = response.content() == null
			? List.of()
			: response.content().stream()
			.map(item -> new PaymentSettlementSource(
				item.paymentId(),
				item.orderId(),
				item.productId(),
				item.paymentStatus(),
				item.totalPaymentAmount(),
				item.occurredAt(),
				item.updatedAt()
			))
			.toList();

		return new SettlementSliceResult<>(
			content,
			response.hasNext(),
			response.nextCursorUpdatedAt(),
			response.nextCursorId()
		);
	}

	@Override
	public SettlementSliceResult<RefundSettlementSource> fetchRefunds(
		LocalDateTime from,
		LocalDateTime to,
		LocalDateTime cursorUpdatedAt,
		UUID cursorId,
		int size
	) {
		URI uri = UriComponentsBuilder
			.fromUriString(clientProperties.payment().baseUrl())
			.path("/api/v1/payments/refunds/settlement-targets")
			.queryParam("from", from)
			.queryParam("to", to)
			.queryParam("cursorUpdatedAt", cursorUpdatedAt)
			.queryParam("cursorId", cursorId)
			.queryParam("size", size)
			.build()
			.toUri();

		RefundSliceResponse response = restTemplate.exchange(
			uri,
			HttpMethod.GET,
			null,
			RefundSliceResponse.class
		).getBody();

		if (response == null) {
			return new SettlementSliceResult<>(List.of(), false, null, null);
		}

		List<RefundSettlementSource> content = response.content() == null
			? List.of()
			: response.content().stream()
			.map(item -> new RefundSettlementSource(
				item.refundId(),
				item.paymentId(),
				item.orderId(),
				item.productId(),
				item.refundStatus(),
				item.totalRefundAmount(),
				item.occurredAt(),
				item.updatedAt()
			))
			.toList();

		return new SettlementSliceResult<>(
			content,
			response.hasNext(),
			response.nextCursorUpdatedAt(),
			response.nextCursorId()
		);
	}

	public record PaymentSliceResponse(
		List<PaymentItem> content,
		boolean hasNext,
		LocalDateTime nextCursorUpdatedAt,
		UUID nextCursorId
	) {
	}

	public record RefundSliceResponse(
		List<RefundItem> content,
		boolean hasNext,
		LocalDateTime nextCursorUpdatedAt,
		UUID nextCursorId
	) {
	}

	public record PaymentItem(
		UUID paymentId,
		UUID orderId,
		UUID productId,
		String paymentStatus,
		BigDecimal totalPaymentAmount,
		LocalDateTime occurredAt,
		LocalDateTime updatedAt
	) {
	}

	public record RefundItem(
		UUID refundId,
		UUID paymentId,
		UUID orderId,
		UUID productId,
		String refundStatus,
		BigDecimal totalRefundAmount,
		LocalDateTime occurredAt,
		LocalDateTime updatedAt
	) {
	}
}
