package jabaclass.payment.application.service;

import jabaclass.payment.application.port.external.OrderPort;
import jabaclass.payment.application.port.external.PaymentGatewayPort;
import jabaclass.payment.application.usecase.PaymentUseCase;
import jabaclass.payment.domain.model.Payment;
import jabaclass.payment.domain.repository.PaymentRepository;
import jabaclass.payment.presentation.dto.request.PreparePaymentRequestDto;
import jabaclass.payment.presentation.dto.request.ConfirmPaymentRequestDto;
import jabaclass.payment.presentation.dto.response.PaymentResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class PaymentService implements PaymentUseCase {

	private final PaymentRepository paymentRepository;
	private final PaymentGatewayPort paymentGatewayPort;
	private final OrderPort orderPort;

	@Override
	@Transactional
	public PaymentResponseDto create(PreparePaymentRequestDto request) {

		// 로그인 사용자
		UUID userId = getCurrentUserId();

		Payment payment = Payment.create(
			userId,
			request.productId(),
			request.orderId(),
			request.paymentMethod(),
			request.paymentAmount(),
			request.depositAmount()
		);

		Payment savedPayment = paymentRepository.save(payment);

		return PaymentResponseDto.from(savedPayment);
	}

	private UUID getCurrentUserId() {
		// TODO: 로그인 기능 구현 후 SecurityContext로 대체
		return UUID.randomUUID();
	}

	@Transactional
	public PaymentResponseDto confirm(ConfirmPaymentRequestDto request) {

		// Payment 조회
		UUID orderId = request.orderId();

		Payment payment = paymentRepository.findByOrderId(orderId)
			.orElseThrow(() -> new IllegalStateException("결제 정보를 찾을 수 없습니다."));

		log.info("[confirm] validate 호출 전 orderId={}, requestAmount={}",
			payment.getOrderId(), request.amount());

		// 멱등성 체크
		if (payment.isDone()) {
			return PaymentResponseDto.from(payment);
		}

		// Order 금액 검증
		boolean valid = orderPort.validateOrder(
			payment.getOrderId(),
			request.amount()
		);
		if (!valid) {
			log.warn("Order 금액 검증 실패. orderId={}, amount={}",
				payment.getOrderId(), request.amount());

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
}
