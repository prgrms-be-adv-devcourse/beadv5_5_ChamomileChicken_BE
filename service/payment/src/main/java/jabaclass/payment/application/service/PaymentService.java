package jabaclass.payment.application.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jabaclass.payment.application.port.external.OrderPort;
import jabaclass.payment.application.port.external.PaymentGatewayPort;
import jabaclass.payment.application.usecase.PaymentSettlementQueryUseCase;
import jabaclass.payment.application.usecase.PaymentUseCase;
import jabaclass.payment.common.error.PaymentErrorCode;
import jabaclass.payment.common.error.PaymentException;
import jabaclass.payment.domain.model.Payment;
import jabaclass.payment.domain.model.Refund;
import jabaclass.payment.domain.repository.PaymentRepository;
import jabaclass.payment.domain.repository.RefundRepository;
import jabaclass.payment.infrastructure.kafka.PaymentCompletedEvent;
import jabaclass.payment.infrastructure.kafka.PaymentFailedEvent;
import jabaclass.payment.infrastructure.kafka.PaymentRefundedEvent;
import jabaclass.payment.infrastructure.outbox.EventType;
import jabaclass.payment.infrastructure.outbox.OutboxEvent;
import jabaclass.payment.infrastructure.outbox.OutboxRepository;
import jabaclass.payment.presentation.dto.request.ConfirmPaymentRequestDto;
import jabaclass.payment.presentation.dto.request.PreparePaymentRequestDto;
import jabaclass.payment.presentation.dto.request.RefundPaymentRequestDto;
import jabaclass.payment.presentation.dto.response.PaymentResponseDto;
import jabaclass.payment.presentation.dto.response.PaymentSettlementSliceResponseDto;
import jabaclass.payment.presentation.dto.response.PaymentSettlementTargetItemResponseDto;
import jabaclass.payment.presentation.dto.response.RefundSettlementSliceResponseDto;
import jabaclass.payment.presentation.dto.response.RefundSettlementTargetItemResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class PaymentService implements PaymentUseCase, PaymentSettlementQueryUseCase {

	private final PaymentRepository paymentRepository;
	private final RefundRepository refundRepository;
	private final PaymentGatewayPort paymentGatewayPort;
	private final OrderPort orderPort;
	private final OutboxRepository outboxRepository;
	private final ObjectMapper objectMapper;

	@Override
	@Transactional
	public PaymentResponseDto create(UUID userId, PreparePaymentRequestDto request) {

		// Payment 생성
		Payment payment = Payment.create(
			userId,
			request.productId(),
			request.orderId(),
			request.productUserId(),
			request.paymentMethod(),
			request.paymentAmount(),
			request.depositAmount()
		);

		// 예치금 100% 결제인 경우 → PG 호출 없이 바로 완료 처리
		if (payment.getPaymentAmount().compareTo(BigDecimal.ZERO) == 0) {
			payment.markDone("DEPOSIT_ONLY");
		}

		Payment savedPayment = paymentRepository.save(payment);

		if (savedPayment.getPaymentAmount().compareTo(BigDecimal.ZERO) == 0) {
			OutboxEvent event = OutboxEvent.create(
				"PAYMENT",
				savedPayment.getId().toString(),
				EventType.PAYMENT_COMPLETED,
				toJson(new PaymentCompletedEvent(UUID.randomUUID(), savedPayment.getId(), savedPayment.getOrderId()))
			);

			outboxRepository.save(event);
		}

		return PaymentResponseDto.from(savedPayment);
	}

	@Transactional
	public PaymentResponseDto confirm(UUID userId, ConfirmPaymentRequestDto request) {

		// Payment 조회
		UUID orderId = request.orderId();

		Payment payment = paymentRepository.findByOrderId(orderId)
			.orElseThrow(() -> new PaymentException(PaymentErrorCode.PAYMENT_NOT_FOUND));

		if (!payment.getUserId().equals(userId)) {
			throw new PaymentException(PaymentErrorCode.UNAUTHORIZED_PAYMENT_ACCESS);
		}

		// 멱등성 처리: 이미 완료된 결제는 다시 처리하지 않음
		if (payment.isDone()) {
			return PaymentResponseDto.from(payment);
		}

		// Order 서비스에 금액 검증 요청
		boolean valid = orderPort.validateOrder(
			payment.getOrderId(),
			payment.getTotalAmount().intValue()
		);
		if (!valid) {
			throw new PaymentException(PaymentErrorCode.INVALID_ORDER_AMOUNT);
		}

		// Payment 내부 금액 검증
		if (payment.getPaymentAmount()
			.compareTo(BigDecimal.valueOf(request.amount())) != 0) {
			throw new PaymentException(PaymentErrorCode.INVALID_PAYMENT_AMOUNT);
		}

		try {
			// 외부 PG 승인 요청
			paymentGatewayPort.confirm(
				request.paymentKey(),
				payment.getOrderId().toString(),
				request.amount()
			);

			// 결제 성공 처리
			payment.markDone(request.paymentKey());

			outboxRepository.save(
				OutboxEvent.create(
					"PAYMENT",
					payment.getId().toString(),
					EventType.PAYMENT_COMPLETED,
					toJson(new PaymentCompletedEvent(UUID.randomUUID(), payment.getId(), payment.getOrderId()))
				)
			);

		} catch (Exception e) {

			// 결제 실패 처리
			payment.markFailed();

			outboxRepository.save(
				OutboxEvent.create(
					"PAYMENT",
					payment.getId().toString(),
					EventType.PAYMENT_FAILED,
					toJson(new PaymentFailedEvent(UUID.randomUUID(), payment.getId(), payment.getOrderId(), payment.getDepositAmount()))
				)
			);

			throw new PaymentException(PaymentErrorCode.PAYMENT_CONFIRM_FAILED, e);
		}

		// 결과 반환
		return PaymentResponseDto.from(payment);
	}

	@Override
	@Transactional
	public void refund(UUID userId, RefundPaymentRequestDto request) {

		// Payment 조회
		Payment payment = paymentRepository.findByOrderId(request.orderId())
			.orElseThrow(() -> new PaymentException(PaymentErrorCode.PAYMENT_NOT_FOUND));

		if (!payment.getUserId().equals(userId)) {
			throw new PaymentException(PaymentErrorCode.UNAUTHORIZED_PAYMENT_ACCESS);
		}

		// 완료된 결제만 환불 가능
		if (!payment.isDone()) {
			throw new PaymentException(PaymentErrorCode.PAYMENT_NOT_COMPLETED);
		}

		Refund refund = Refund.create(
			payment.getId(),
			null,
			payment.getPaymentAmount(),
			payment.getDepositAmount()
		);
		refundRepository.save(refund);

		try {
			// 실제 결제 금액이 있는 경우 PG 환불 호출
			if (payment.getPaymentAmount().signum() > 0) {
				paymentGatewayPort.refund(
					payment.getPaymentKey(),
					payment.getPaymentAmount().intValue()
				);
			}

			// 상태 변경
			payment.markCancelled();
			refund.markCompleted();
			refundRepository.save(refund);

			outboxRepository.save(
				OutboxEvent.create(
					"PAYMENT",
					payment.getId().toString(),
					EventType.PAYMENT_REFUNDED,
					toJson(new PaymentRefundedEvent(UUID.randomUUID(), payment.getId(), payment.getOrderId()))
				)
			);

		} catch (Exception e) {
			// 환불 실패 처리
			refund.markFailed();
			refundRepository.save(refund);

			throw new PaymentException(PaymentErrorCode.PAYMENT_REFUND_FAILED, e);
		}
	}

	// Outbox payload 직렬화
	// - 이벤트 객체 → JSON 문자열로 변환
	// - Kafka 전송 시 payload로 사용됨
	private String toJson(Object obj) {
		try {
			return objectMapper.writeValueAsString(obj);
		} catch (Exception e) {
			throw new RuntimeException("Outbox 직렬화 실패", e);
		}
	}

	@Override
	public PaymentSettlementSliceResponseDto getPaymentSettlementTargets(
		LocalDateTime from,
		LocalDateTime to,
		LocalDateTime cursorUpdatedAt,
		UUID cursorId,
		int size
	) {
		List<PaymentRepository.SettlementPaymentSource> sources = paymentRepository.findSettlementPaymentSources(
			from,
			to,
			cursorUpdatedAt,
			cursorId,
			size + 1
		);

		boolean hasNext = sources.size() > size;
		List<PaymentRepository.SettlementPaymentSource> page = hasNext ? sources.subList(0, size) : sources;

		LocalDateTime nextCursorUpdatedAt = hasNext ? page.get(page.size() - 1).occurredAt() : null;
		UUID nextCursorId = hasNext ? page.get(page.size() - 1).paymentId() : null;

		return new PaymentSettlementSliceResponseDto(
			page.stream()
				.map(item -> new PaymentSettlementTargetItemResponseDto(
					item.paymentId(),
					item.orderId(),
					item.productId(),
					item.paymentStatus(),
					item.totalPaymentAmount(),
					item.occurredAt(),
					item.occurredAt()
				))
				.toList(),
			hasNext,
			nextCursorUpdatedAt,
			nextCursorId
		);
	}

	@Override
	public RefundSettlementSliceResponseDto getRefundSettlementTargets(
		LocalDateTime from,
		LocalDateTime to,
		LocalDateTime cursorUpdatedAt,
		UUID cursorId,
		int size
	) {
		List<RefundRepository.SettlementRefundSource> sources = refundRepository.findSettlementRefundSources(
			from,
			to,
			cursorUpdatedAt,
			cursorId,
			size + 1
		);

		boolean hasNext = sources.size() > size;
		List<RefundRepository.SettlementRefundSource> page = hasNext ? sources.subList(0, size) : sources;

		LocalDateTime nextCursorUpdatedAt = hasNext ? page.get(page.size() - 1).occurredAt() : null;
		UUID nextCursorId = hasNext ? page.get(page.size() - 1).refundId() : null;

		return new RefundSettlementSliceResponseDto(
			page.stream()
				.map(item -> new RefundSettlementTargetItemResponseDto(
					item.refundId(),
					item.paymentId(),
					item.orderId(),
					item.productId(),
					item.refundStatus(),
					item.totalRefundAmount(),
					item.occurredAt(),
					item.occurredAt()
				))
				.toList(),
			hasNext,
			nextCursorUpdatedAt,
			nextCursorId
		);
	}
}
