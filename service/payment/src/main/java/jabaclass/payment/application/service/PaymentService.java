package jabaclass.payment.application.service;

import java.math.BigDecimal;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jabaclass.payment.application.port.external.OrderPort;
import jabaclass.payment.application.port.external.PaymentGatewayPort;
import jabaclass.payment.application.port.external.UserPort;
import jabaclass.payment.application.usecase.PaymentUseCase;
import jabaclass.payment.domain.model.Payment;
import jabaclass.payment.domain.model.Refund;
import jabaclass.payment.domain.repository.PaymentRepository;
import jabaclass.payment.domain.repository.RefundRepository;
import jabaclass.payment.infrastructure.kafka.PaymentRefundCompletedEvent;
import jabaclass.payment.infrastructure.kafka.PaymentRefundCompletedEventPublisher;
import jabaclass.payment.presentation.dto.request.ConfirmPaymentRequestDto;
import jabaclass.payment.presentation.dto.request.PreparePaymentRequestDto;
import jabaclass.payment.presentation.dto.request.RefundPaymentRequestDto;
import jabaclass.payment.presentation.dto.response.PaymentResponseDto;
import jabaclass.payment.presentation.dto.response.RefundPaymentResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class PaymentService implements PaymentUseCase {

	private final PaymentRepository paymentRepository;
	private final RefundRepository refundRepository;
	private final PaymentGatewayPort paymentGatewayPort;
	private final OrderPort orderPort;
	private final UserPort userPort;
	private final PaymentRefundCompletedEventPublisher refundCompletedEventPublisher;

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

			try {
				orderPort.updatePaymentStatus(
					payment.getOrderId(),
					payment.getId(),
					payment.getDepositAmount().intValue(),
					"PAID"
				);
			} catch (Exception ex) {
				log.error("Order 상태 업데이트 실패 (deposit only)", ex);
			}
		}

		Payment savedPayment = paymentRepository.save(payment);

		return PaymentResponseDto.from(savedPayment);
	}

	@Transactional
	public PaymentResponseDto confirm(ConfirmPaymentRequestDto request) {

		// Payment 조회
		UUID orderId = request.orderId();

		Payment payment = paymentRepository.findByOrderId(orderId)
			.orElseThrow(() -> new IllegalStateException("결제 정보를 찾을 수 없습니다."));

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

			throw new IllegalStateException("주문 금액이 일치하지 않습니다.");
		}

		// Payment 내부 금액 검증
		if (payment.getPaymentAmount()
			.compareTo(BigDecimal.valueOf(request.amount())) != 0) {
			throw new IllegalStateException("결제 금액이 일치하지 않습니다.");
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

			try {
				orderPort.updatePaymentStatus(
					payment.getOrderId(),
					payment.getId(),
					payment.getDepositAmount().intValue(),
					"PAID"
				);
			} catch (Exception ex) {
				log.error("Order 상태 업데이트 실패 (PAID). orderId={}",
					payment.getOrderId(), ex);
			}

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

			throw new IllegalStateException("결제 승인에 실패했습니다.", e);
		}

		// 결과 반환
		return PaymentResponseDto.from(payment);
	}

	@Override
	@Transactional
	public RefundPaymentResponseDto refund(RefundPaymentRequestDto request) {
		Payment payment = paymentRepository.findByOrderId(request.orderId())
			.orElseThrow(() -> new IllegalStateException("결제 정보를 찾을 수 없습니다."));

		if (!payment.isDone()) {
			throw new IllegalStateException("완료된 결제만 환불할 수 있습니다.");
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
			throw new IllegalStateException("환불 처리에 실패했습니다.", e);
		}

		return RefundPaymentResponseDto.from(refund, payment.getOrderId());
	}
}
