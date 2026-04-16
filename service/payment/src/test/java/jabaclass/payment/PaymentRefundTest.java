package jabaclass.payment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.fasterxml.jackson.databind.ObjectMapper;

import jabaclass.payment.application.port.external.OrderPort;
import jabaclass.payment.application.port.external.PaymentGatewayPort;
import jabaclass.payment.application.service.PaymentService;
import jabaclass.payment.application.service.handler.PaymentConfirmHandler;
import jabaclass.payment.common.error.PaymentErrorCode;
import jabaclass.payment.common.error.PaymentException;
import jabaclass.payment.domain.model.Payment;
import jabaclass.payment.domain.model.PaymentMethod;
import jabaclass.payment.domain.model.PaymentStatus;
import jabaclass.payment.domain.model.Refund;
import jabaclass.payment.domain.model.RefundStatus;
import jabaclass.payment.domain.repository.PaymentRepository;
import jabaclass.payment.domain.repository.RefundRepository;
import jabaclass.payment.infrastructure.outbox.EventType;
import jabaclass.payment.infrastructure.outbox.OutboxEvent;
import jabaclass.payment.infrastructure.outbox.OutboxRepository;
import jabaclass.payment.presentation.dto.request.RefundPaymentRequestDto;

@ExtendWith(MockitoExtension.class)
class PaymentRefundTest {
	private static final UUID TEST_USER_ID = UUID.randomUUID();

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

	private PaymentService paymentService;

	@BeforeEach
	void setUp() {
		paymentService = new PaymentService(
			paymentRepository,
			refundRepository,
			paymentGatewayPort,
			orderPort,
			outboxRepository,
			paymentConfirmHandler,
			new ObjectMapper()
		);

		lenient().when(refundRepository.save(any(Refund.class)))
			.thenAnswer(invocation -> invocation.getArgument(0));
		lenient().when(outboxRepository.save(any(OutboxEvent.class)))
			.thenAnswer(invocation -> invocation.getArgument(0));
	}

	@Test
	void refund_결제가없으면_예외() {
		UUID orderId = UUID.randomUUID();
		when(paymentRepository.findByOrderId(orderId)).thenReturn(Optional.empty());

		PaymentException exception = assertThrows(
			PaymentException.class,
			() -> paymentService.refund(TEST_USER_ID, new RefundPaymentRequestDto(orderId))
		);

		assertEquals(PaymentErrorCode.PAYMENT_NOT_FOUND, exception.getErrorCode());
		verify(refundRepository, never()).save(any(Refund.class));
		verify(outboxRepository, never()).save(any(OutboxEvent.class));
	}

	@Test
	void refund_완료되지않은결제면_예외() {
		UUID orderId = UUID.randomUUID();
		Payment payment = createPayment(TEST_USER_ID, orderId, BigDecimal.valueOf(10000), BigDecimal.ZERO);
		assignPaymentId(payment);
		when(paymentRepository.findByOrderId(orderId)).thenReturn(Optional.of(payment));

		PaymentException exception = assertThrows(
			PaymentException.class,
			() -> paymentService.refund(TEST_USER_ID, new RefundPaymentRequestDto(orderId))
		);

		assertEquals(PaymentErrorCode.PAYMENT_NOT_COMPLETED, exception.getErrorCode());
		verify(refundRepository, never()).save(any(Refund.class));
		verify(outboxRepository, never()).save(any(OutboxEvent.class));
	}

	@Test
	void refund_일반결제환불성공시_PG호출후_환불완료() {
		UUID orderId = UUID.randomUUID();
		Payment payment = createPayment(TEST_USER_ID, orderId, BigDecimal.valueOf(10000), BigDecimal.ZERO);
		assignPaymentId(payment);
		payment.markDone("pay-key");
		when(paymentRepository.findByOrderId(orderId)).thenReturn(Optional.of(payment));

		paymentService.refund(TEST_USER_ID, new RefundPaymentRequestDto(orderId));

		verify(paymentGatewayPort).refund("pay-key", 10000);
		assertEquals(PaymentStatus.CANCELLED, payment.getStatus());

		ArgumentCaptor<Refund> refundCaptor = ArgumentCaptor.forClass(Refund.class);
		verify(refundRepository, times(2)).save(refundCaptor.capture());
		assertEquals(RefundStatus.COMPLETED, refundCaptor.getAllValues().get(1).getStatus());

		ArgumentCaptor<OutboxEvent> outboxCaptor = ArgumentCaptor.forClass(OutboxEvent.class);
		verify(outboxRepository).save(outboxCaptor.capture());
		assertEquals(EventType.PAYMENT_REFUNDED, outboxCaptor.getValue().getEventType());
	}

	@Test
	void refund_예치금전액결제환불이면_PG호출없음() {
		UUID orderId = UUID.randomUUID();
		Payment payment = createPayment(TEST_USER_ID, orderId, BigDecimal.ZERO, BigDecimal.valueOf(10000));
		assignPaymentId(payment);
		payment.markDone("DEPOSIT_ONLY");
		when(paymentRepository.findByOrderId(orderId)).thenReturn(Optional.of(payment));

		paymentService.refund(TEST_USER_ID, new RefundPaymentRequestDto(orderId));

		verify(paymentGatewayPort, never()).refund(any(), anyInt());
		assertEquals(PaymentStatus.CANCELLED, payment.getStatus());

		ArgumentCaptor<Refund> refundCaptor = ArgumentCaptor.forClass(Refund.class);
		verify(refundRepository, times(2)).save(refundCaptor.capture());
		assertEquals(RefundStatus.COMPLETED, refundCaptor.getAllValues().get(1).getStatus());
		verify(outboxRepository).save(any(OutboxEvent.class));
	}

	@Test
	void refund_PG실패면_환불실패로저장하고_예외() {
		UUID orderId = UUID.randomUUID();
		Payment payment = createPayment(TEST_USER_ID, orderId, BigDecimal.valueOf(10000), BigDecimal.ZERO);
		assignPaymentId(payment);
		payment.markDone("pay-key");
		when(paymentRepository.findByOrderId(orderId)).thenReturn(Optional.of(payment));
		doThrow(new RuntimeException("refund failed"))
			.when(paymentGatewayPort).refund("pay-key", 10000);

		PaymentException exception = assertThrows(
			PaymentException.class,
			() -> paymentService.refund(TEST_USER_ID, new RefundPaymentRequestDto(orderId))
		);

		assertEquals(PaymentErrorCode.PAYMENT_REFUND_FAILED, exception.getErrorCode());
		assertEquals(PaymentStatus.PAID, payment.getStatus());

		ArgumentCaptor<Refund> refundCaptor = ArgumentCaptor.forClass(Refund.class);
		verify(refundRepository, times(2)).save(refundCaptor.capture());
		assertEquals(RefundStatus.FAILED, refundCaptor.getAllValues().get(1).getStatus());
		verify(outboxRepository, never()).save(any(OutboxEvent.class));
	}

	private Payment createPayment(UUID userId, UUID orderId, BigDecimal paymentAmount, BigDecimal depositAmount) {
		return Payment.create(
			userId,
			UUID.randomUUID(),
			orderId,
			UUID.randomUUID(),
			PaymentMethod.TOSS,
			paymentAmount,
			depositAmount
		);
	}

	private void assignPaymentId(Payment payment) {
		ReflectionTestUtils.setField(payment, "id", UUID.randomUUID());
	}
}
