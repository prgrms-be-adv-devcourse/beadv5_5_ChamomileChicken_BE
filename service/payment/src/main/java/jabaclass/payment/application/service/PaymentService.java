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
import jabaclass.payment.common.exception.PaymentErrorCode;
import jabaclass.payment.common.exception.PaymentException;
import jabaclass.payment.domain.model.Payment;
import jabaclass.payment.domain.model.Refund;
import jabaclass.payment.domain.repository.PaymentRepository;
import jabaclass.payment.domain.repository.RefundRepository;
import jabaclass.payment.infrastructure.kafka.PaymentCompletedEvent;
import jabaclass.payment.infrastructure.kafka.PaymentCompletedEventPublisher;
import jabaclass.payment.infrastructure.kafka.PaymentRefundCompletedEvent;
import jabaclass.payment.infrastructure.kafka.PaymentRefundCompletedEventPublisher;
import jabaclass.payment.presentation.dto.request.ConfirmPaymentRequestDto;
import jabaclass.payment.presentation.dto.request.PreparePaymentRequestDto;
import jabaclass.payment.presentation.dto.request.RefundPaymentRequestDto;
import jabaclass.payment.presentation.dto.response.PaymentResponseDto;
import jabaclass.payment.presentation.dto.response.PaymentSettlementSliceResponseDto;
import jabaclass.payment.presentation.dto.response.PaymentSettlementTargetItemResponseDto;
import jabaclass.payment.presentation.dto.response.RefundPaymentResponseDto;
import jabaclass.payment.presentation.dto.response.RefundSettlementSliceResponseDto;
import jabaclass.payment.presentation.dto.response.RefundSettlementTargetItemResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class PaymentService implements PaymentUseCase, PaymentSettlementQueryUseCase {

	private final PaymentRepository paymentRepository;
	private final RefundRepository refundRepository;
	private final PaymentGatewayPort paymentGatewayPort;
	private final OrderPort orderPort;
	private final PaymentRefundCompletedEventPublisher refundCompletedEventPublisher;
	private final PaymentCompletedEventPublisher paymentCompletedEventPublisher;

	@Override
	@Transactional
	public PaymentResponseDto create(PreparePaymentRequestDto request) {
		Payment payment = Payment.create(
			request.userId(),
			request.productId(),
			request.orderId(),
			request.productUserId(),
			request.paymentMethod(),
			request.paymentAmount(),
			request.depositAmount()
		);

		if (payment.getPaymentAmount().compareTo(BigDecimal.ZERO) == 0) {

			log.info("예치금 100% 결제 - 즉시 완료 처리 orderId={}", payment.getOrderId());

			payment.markDone("DEPOSIT_ONLY");

			paymentCompletedEventPublisher.publish(
				new PaymentCompletedEvent(
					payment.getOrderId(),
					payment.getId()
				)
			);
		}

		Payment savedPayment = paymentRepository.save(payment);

		return PaymentResponseDto.from(savedPayment);
	}

	@Transactional
	public PaymentResponseDto confirm(ConfirmPaymentRequestDto request) {

		// Payment 조회
		UUID orderId = request.orderId();

		Payment payment = paymentRepository.findByOrderId(orderId)
			.orElseThrow(() -> new PaymentException(PaymentErrorCode.PAYMENT_NOT_FOUND));

		log.info("[confirm] validate 호출 전 orderId={}, requestAmount={}, totalAmount={}",
			payment.getOrderId(), request.amount(), payment.getTotalAmount());

		// 멱등성 체크
		if (payment.isDone()) {
			return PaymentResponseDto.from(payment);
		}

		// Order 금액 검증
		boolean valid = orderPort.validateOrder(
			payment.getOrderId(),
			payment.getTotalAmount().intValue()
		);
		if (!valid) {
			log.warn("Order 금액 검증 실패. orderId={}, totalAmount={}, requestAmount={}",
				payment.getOrderId(), payment.getTotalAmount(), request.amount());

			throw new PaymentException(PaymentErrorCode.INVALID_ORDER_AMOUNT);
		}

		// Payment 내부 금액 검증
		if (payment.getPaymentAmount()
			.compareTo(BigDecimal.valueOf(request.amount())) != 0) {
			throw new PaymentException(PaymentErrorCode.INVALID_PAYMENT_AMOUNT);
		}

		try {
			// PG 승인
			paymentGatewayPort.confirm(
				request.paymentKey(),
				payment.getOrderId().toString(),
				request.amount()
			);

			// Payment 상태 변경
			payment.markDone(request.paymentKey());

			paymentCompletedEventPublisher.publish(
				new PaymentCompletedEvent(
					payment.getOrderId(),
					payment.getId()
				)
			);

		} catch (Exception e) {

			log.error("결제 승인 실패. paymentId={}, orderId={}",
				payment.getId(), payment.getOrderId(), e);

			// Payment 실패 처리
			payment.markFailed();

			try {
				// Order 실패 상태 반영 (보조 처리)
				orderPort.updatePaymentStatus(
					payment.getOrderId(),
					payment.getId(),
					payment.getDepositAmount().intValue(),
					"FAILED"
				);
			} catch (Exception ex) {
				log.error("Order 상태 업데이트 실패 (FAILED). orderId={}",
					payment.getOrderId(), ex);
			}

			throw new PaymentException(PaymentErrorCode.PAYMENT_CONFIRM_FAILED, e);
		}

		// 결과 반환
		return PaymentResponseDto.from(payment);
	}

	/*@Override
	@Transactional
	public RefundPaymentResponseDto refund(RefundPaymentRequestDto request) {
		Payment payment = paymentRepository.findByOrderId(request.orderId())
			.orElseThrow(() -> new PaymentException(PaymentErrorCode.PAYMENT_NOT_FOUND));

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
		refund.markProcessing();

		try {
			if (payment.getPaymentAmount().signum() > 0) { // 결제수단 금액이 있으면 Toss 환불 호출
				paymentGatewayPort.refund(
					payment.getPaymentKey(),
					payment.getPaymentAmount().intValue()
				);
			}

			if (payment.getDepositAmount().signum() > 0) { // 예치금 금액이 있으면 User 서비스에 환불 호출
				userPort.refundDeposit(
					payment.getUserId(),
					payment.getDepositAmount(),
					payment.getId()
				);
			}

			payment.markCancelled();
			refund.markCompleted();
			refundRepository.save(refund);

			refundCompletedEventPublisher.publish(
				new PaymentRefundCompletedEvent(
					payment.getOrderId(),
					payment.getProductUserId()
				)
			);
		} catch (Exception e) {
			refund.markFailed();
			refundRepository.save(refund);
			log.error("환불 실패. orderId={}, paymentId={}", payment.getOrderId(), payment.getId(), e);

			throw new PaymentException(PaymentErrorCode.PAYMENT_REFUND_FAILED, e);
		}

		return RefundPaymentResponseDto.from(refund, payment.getOrderId());
	}*/

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
