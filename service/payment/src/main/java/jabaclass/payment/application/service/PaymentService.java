package jabaclass.payment.application.service;

import jabaclass.payment.application.port.out.OrderPort;
import jabaclass.payment.application.port.out.PaymentGatewayPort;
import jabaclass.payment.application.usecase.PaymentUseCase;
import jabaclass.payment.domain.model.Payment;
import jabaclass.payment.domain.repository.PaymentRepository;
import jabaclass.payment.presentation.dto.request.PreparePaymentRequestDto;
import jabaclass.payment.presentation.dto.request.ConfirmPaymentRequestDto;
import jabaclass.payment.presentation.dto.response.PaymentResponseDto;
import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
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
		Payment payment = paymentRepository.findById(request.paymentId())
			.orElseThrow(() -> new IllegalArgumentException("결제 정보를 찾을 수 없습니다."));

		// 멱등성 체크
		if (payment.isDone()) {
			return PaymentResponseDto.from(payment);
		}

		// TODO: Order Service api 명세 확정 후 추가 예정
		// Order 검증
		/*boolean valid = orderPort.validateOrder(
			payment.getOrderId(),
			request.amount()
		);
		if (!valid) {
			throw new IllegalArgumentException("주문 금액 검증이 실패하였습니다.");
		}*/

		// 금액 검증
		if (payment.getPaymentAmount()
			.compareTo(BigDecimal.valueOf(request.amount())) != 0) {
			throw new IllegalArgumentException("결제 금액이 일치하지 않습니다.");
		}

		// Toss 승인 API 호출
		paymentGatewayPort.confirm(
			request.paymentKey(),
			payment.getOrderId().toString(),
			request.amount()
		);

		// Payment 상태 변경
		payment.markDone(request.paymentKey());

		// TODO: Order Service api 명세 확정 후 추가 예정
		// Order 업데이트
		/*orderPort.updatePaymentStatus(
			payment.getOrderId(),
			payment.getId(),
			"SUCCESS"
		);*/

		// 결과 반환
		return PaymentResponseDto.from(payment);
	}
}
