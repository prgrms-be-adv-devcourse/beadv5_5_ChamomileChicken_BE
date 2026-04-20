package jabaclass.payment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.fasterxml.jackson.databind.ObjectMapper;

import jabaclass.payment.application.port.external.OrderPort;
import jabaclass.payment.application.port.external.PaymentGatewayPort;
import jabaclass.payment.application.service.PaymentService;
import jabaclass.payment.application.service.handler.PaymentConfirmHandler;
import jabaclass.payment.application.service.handler.PaymentRefundHandler;
import jabaclass.payment.common.error.PaymentErrorCode;
import jabaclass.payment.common.error.PaymentException;
import jabaclass.payment.domain.model.Payment;
import jabaclass.payment.domain.model.PaymentMethod;
import jabaclass.payment.domain.repository.PaymentRepository;
import jabaclass.payment.domain.repository.RefundRepository;
import jabaclass.payment.infrastructure.outbox.OutboxRepository;
import jabaclass.payment.presentation.dto.request.InternalRefundRequestDto;
import jabaclass.payment.presentation.dto.response.InternalRefundResponseDto;

@ExtendWith(MockitoExtension.class)
class PaymentRefundTest {

	@Mock
	private PaymentRepository paymentRepository;

	@Mock
	private RefundRepository refundRepository;

	@Mock
	private PaymentGatewayPort paymentGatewayPort;

	@Mock
	private OrderPort orderPort;

	@Mock
	private OutboxRepository outboxRepository;

	@Mock
	private PaymentConfirmHandler paymentConfirmHandler;

	@Mock
	private PaymentRefundHandler paymentRefundHandler;

	private PaymentService paymentService;

	private static final BigDecimal FULL_REFUND_RATE = BigDecimal.ONE;

	@BeforeEach
	void setUp() {
		paymentService = new PaymentService(
			paymentRepository,
			refundRepository,
			paymentGatewayPort,
			orderPort,
			outboxRepository,
			paymentConfirmHandler,
			paymentRefundHandler,
			new ObjectMapper()
		);

		lenient().when(outboxRepository.save(any()))
			.thenAnswer(invocation -> invocation.getArgument(0));
	}

	@Test
	void refundByOrder_결제가없으면_예외() {
		UUID orderId = UUID.randomUUID();
		when(paymentRepository.findByOrderId(orderId)).thenReturn(Optional.empty());

		PaymentException exception = assertThrows(
			PaymentException.class,
			() -> paymentService.refundByOrder(new InternalRefundRequestDto(orderId, FULL_REFUND_RATE))
		);

		assertEquals(PaymentErrorCode.PAYMENT_NOT_FOUND, exception.getErrorCode());
		verify(paymentRefundHandler, never()).onSuccess(any(), any());
	}

	@Test
	void refundByOrder_완료되지않은결제면_예외() {
		UUID orderId = UUID.randomUUID();
		Payment payment = createPayment(orderId, BigDecimal.valueOf(10000), BigDecimal.ZERO);
		assignPaymentId(payment);
		when(paymentRepository.findByOrderId(orderId)).thenReturn(Optional.of(payment));

		PaymentException exception = assertThrows(
			PaymentException.class,
			() -> paymentService.refundByOrder(new InternalRefundRequestDto(orderId, FULL_REFUND_RATE))
		);

		assertEquals(PaymentErrorCode.PAYMENT_NOT_COMPLETED, exception.getErrorCode());
		verify(paymentRefundHandler, never()).onSuccess(any(), any());
	}

	@Test
	void refundByOrder_일반결제_성공시_PG호출후_핸들러위임() {
		UUID orderId = UUID.randomUUID();
		Payment payment = createPayment(orderId, BigDecimal.valueOf(10000), BigDecimal.ZERO);
		assignPaymentId(payment);
		payment.markDone("pay-key");
		InternalRefundResponseDto refundResponse = refundResponse(payment.getId(), payment.getProductId(), BigDecimal.ZERO);
		when(paymentRepository.findByOrderId(orderId)).thenReturn(Optional.of(payment));
		when(paymentRefundHandler.onSuccess(payment.getId(), FULL_REFUND_RATE)).thenReturn(refundResponse);

		InternalRefundResponseDto response = paymentService.refundByOrder(
			new InternalRefundRequestDto(orderId, FULL_REFUND_RATE)
		);

		// PG 환불 호출 시 orderId를 멱등키로 전달
		verify(paymentGatewayPort).refund(eq("pay-key"), eq(10000), eq(orderId.toString()));
		verify(paymentRefundHandler).onSuccess(payment.getId(), FULL_REFUND_RATE);
		assertEquals(refundResponse, response);
	}

	@Test
	void refundByOrder_예치금전액결제_PG호출없이_핸들러위임() {
		UUID orderId = UUID.randomUUID();
		Payment payment = createPayment(orderId, BigDecimal.ZERO, BigDecimal.valueOf(10000));
		assignPaymentId(payment);
		payment.markDone("DEPOSIT_ONLY");
		InternalRefundResponseDto refundResponse = refundResponse(
			payment.getId(),
			payment.getProductId(),
			BigDecimal.valueOf(10000)
		);
		when(paymentRepository.findByOrderId(orderId)).thenReturn(Optional.of(payment));
		when(paymentRefundHandler.onSuccess(payment.getId(), FULL_REFUND_RATE)).thenReturn(refundResponse);

		InternalRefundResponseDto response = paymentService.refundByOrder(
			new InternalRefundRequestDto(orderId, FULL_REFUND_RATE)
		);

		// paymentAmount == 0 이므로 PG 호출 없음
		verify(paymentGatewayPort, never()).refund(any(), any(Integer.class), any());
		verify(paymentRefundHandler).onSuccess(payment.getId(), FULL_REFUND_RATE);
		assertEquals(refundResponse, response);
	}

	@Test
	void refundByOrder_PG실패면_REFUND_FAILED예외() {
		UUID orderId = UUID.randomUUID();
		Payment payment = createPayment(orderId, BigDecimal.valueOf(10000), BigDecimal.ZERO);
		assignPaymentId(payment);
		payment.markDone("pay-key");
		when(paymentRepository.findByOrderId(orderId)).thenReturn(Optional.of(payment));
		doThrow(new RuntimeException("pg error"))
			.when(paymentGatewayPort).refund(any(), any(Integer.class), any());

		PaymentException exception = assertThrows(
			PaymentException.class,
			() -> paymentService.refundByOrder(new InternalRefundRequestDto(orderId, FULL_REFUND_RATE))
		);

		assertEquals(PaymentErrorCode.PAYMENT_REFUND_FAILED, exception.getErrorCode());
		verify(paymentRefundHandler, never()).onSuccess(any(), any());
	}

	private Payment createPayment(UUID orderId, BigDecimal paymentAmount, BigDecimal depositAmount) {
		return Payment.create(
			UUID.randomUUID(),
			UUID.randomUUID(),
			orderId,
			PaymentMethod.TOSS,
			paymentAmount,
			depositAmount
		);
	}

	private void assignPaymentId(Payment payment) {
		ReflectionTestUtils.setField(payment, "id", UUID.randomUUID());
	}

	private InternalRefundResponseDto refundResponse(UUID paymentId, UUID productId, BigDecimal depositRefundAmount) {
		return new InternalRefundResponseDto(
			UUID.randomUUID(),
			paymentId,
			productId,
			depositRefundAmount,
			BigDecimal.valueOf(10000).add(depositRefundAmount),
			LocalDateTime.of(2026, 4, 20, 15, 30)
		);
	}
}
