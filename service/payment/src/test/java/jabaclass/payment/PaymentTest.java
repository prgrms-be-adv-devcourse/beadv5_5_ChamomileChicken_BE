package jabaclass.payment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
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
import static org.mockito.Mockito.lenient;

import jabaclass.payment.application.port.external.OrderPort;
import jabaclass.payment.application.port.external.PaymentGatewayPort;
import jabaclass.payment.application.service.PaymentService;
import jabaclass.payment.application.service.handler.PaymentConfirmHandler;
import jabaclass.payment.common.error.PaymentErrorCode;
import jabaclass.payment.common.error.PaymentException;
import jabaclass.payment.domain.model.Payment;
import jabaclass.payment.domain.model.PaymentMethod;
import jabaclass.payment.domain.model.PaymentStatus;
import jabaclass.payment.domain.repository.PaymentRepository;
import jabaclass.payment.domain.repository.RefundRepository;
import jabaclass.payment.infrastructure.outbox.EventType;
import jabaclass.payment.infrastructure.outbox.OutboxEvent;
import jabaclass.payment.infrastructure.outbox.OutboxRepository;
import jabaclass.payment.presentation.dto.request.ConfirmPaymentRequestDto;
import jabaclass.payment.presentation.dto.request.PreparePaymentRequestDto;
import jabaclass.payment.presentation.dto.response.PaymentResponseDto;

@ExtendWith(MockitoExtension.class)
class PaymentTest {
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

		lenient().when(paymentRepository.save(any(Payment.class)))
			.thenAnswer(invocation -> invocation.getArgument(0));
		lenient().when(outboxRepository.save(any(OutboxEvent.class)))
			.thenAnswer(invocation -> invocation.getArgument(0));
	}

	@Test
	void create_예치금전액결제면_즉시완료_아웃박스저장() {
		PreparePaymentRequestDto request = prepareRequest(BigDecimal.ZERO, BigDecimal.valueOf(10000));
		UUID paymentId = UUID.randomUUID();
		when(paymentRepository.save(any(Payment.class)))
			.thenAnswer(invocation -> {
				Payment payment = invocation.getArgument(0);
				ReflectionTestUtils.setField(payment, "id", paymentId);
				return payment;
			});

		PaymentResponseDto response = paymentService.create(TEST_USER_ID, request);

		ArgumentCaptor<Payment> paymentCaptor = ArgumentCaptor.forClass(Payment.class);
		verify(paymentRepository).save(paymentCaptor.capture());
		Payment savedPayment = paymentCaptor.getValue();

		assertTrue(savedPayment.isDone());
		assertEquals(PaymentStatus.PAID, savedPayment.getStatus());
		assertEquals("DEPOSIT_ONLY", savedPayment.getPaymentKey());
		assertEquals(paymentId, response.paymentId());
		assertEquals(BigDecimal.valueOf(10000), response.totalAmount());

		ArgumentCaptor<OutboxEvent> outboxCaptor = ArgumentCaptor.forClass(OutboxEvent.class);
		verify(outboxRepository).save(outboxCaptor.capture());
		assertEquals(EventType.PAYMENT_COMPLETED, outboxCaptor.getValue().getEventType());
	}

	@Test
	void create_일반결제면_준비상태로저장_아웃박스미생성() {
		PreparePaymentRequestDto request = prepareRequest(BigDecimal.valueOf(7000), BigDecimal.valueOf(3000));
		UUID paymentId = UUID.randomUUID();
		when(paymentRepository.save(any(Payment.class)))
			.thenAnswer(invocation -> {
				Payment payment = invocation.getArgument(0);
				ReflectionTestUtils.setField(payment, "id", paymentId);
				return payment;
			});

		PaymentResponseDto response = paymentService.create(TEST_USER_ID, request);

		ArgumentCaptor<Payment> paymentCaptor = ArgumentCaptor.forClass(Payment.class);
		verify(paymentRepository).save(paymentCaptor.capture());
		Payment savedPayment = paymentCaptor.getValue();

		assertFalse(savedPayment.isDone());
		assertEquals(PaymentStatus.READY, savedPayment.getStatus());
		assertEquals(paymentId, response.paymentId());
		assertEquals(BigDecimal.valueOf(10000), response.totalAmount());
		verify(outboxRepository, never()).save(any(OutboxEvent.class));
	}

	@Test
	void confirm_이미완료된결제면_기존결과반환_외부호출없음() {
		UUID orderId = UUID.randomUUID();
		Payment payment = createPayment(TEST_USER_ID, orderId, BigDecimal.valueOf(10000), BigDecimal.ZERO);
		assignPaymentId(payment);
		payment.markDone("existing-key");

		when(paymentRepository.findByOrderId(orderId)).thenReturn(Optional.of(payment));

		PaymentResponseDto response = paymentService.confirm(
			TEST_USER_ID,
			new ConfirmPaymentRequestDto(orderId, "ignored-key", 10000)
		);

		assertEquals(payment.getOrderId(), response.orderId());
		verify(orderPort, never()).validateOrder(any(), anyInt());
		verify(paymentGatewayPort, never()).confirm(any(), any(), anyInt());
		verify(paymentConfirmHandler, never()).onSuccess(any(), any());
		verify(paymentConfirmHandler, never()).onFailure(any(), any(), any());
	}

	@Test
	void confirm_주문금액검증실패면_예외() {
		UUID orderId = UUID.randomUUID();
		Payment payment = createPayment(TEST_USER_ID, orderId, BigDecimal.valueOf(10000), BigDecimal.ZERO);
		assignPaymentId(payment);
		when(paymentRepository.findByOrderId(orderId)).thenReturn(Optional.of(payment));
		when(orderPort.validateOrder(orderId, 10000)).thenReturn(false);

		PaymentException exception = assertThrows(
			PaymentException.class,
			() -> paymentService.confirm(TEST_USER_ID, new ConfirmPaymentRequestDto(orderId, "pay-key", 10000))
		);

		assertEquals(PaymentErrorCode.INVALID_ORDER_AMOUNT, exception.getErrorCode());
		verify(paymentGatewayPort, never()).confirm(any(), any(), anyInt());
		verify(paymentConfirmHandler, never()).onSuccess(any(), any());
	}

	@Test
	void confirm_결제금액불일치면_예외() {
		UUID orderId = UUID.randomUUID();
		Payment payment = createPayment(TEST_USER_ID, orderId, BigDecimal.valueOf(10000), BigDecimal.ZERO);
		assignPaymentId(payment);
		when(paymentRepository.findByOrderId(orderId)).thenReturn(Optional.of(payment));
		when(orderPort.validateOrder(orderId, 10000)).thenReturn(true);

		PaymentException exception = assertThrows(
			PaymentException.class,
			() -> paymentService.confirm(TEST_USER_ID, new ConfirmPaymentRequestDto(orderId, "pay-key", 9000))
		);

		assertEquals(PaymentErrorCode.INVALID_PAYMENT_AMOUNT, exception.getErrorCode());
		verify(paymentGatewayPort, never()).confirm(any(), any(), anyInt());
		verify(paymentConfirmHandler, never()).onSuccess(any(), any());
	}

	@Test
	void confirm_성공하면_paymentConfirmHandler_onSuccess_호출() {
		UUID orderId = UUID.randomUUID();
		Payment payment = createPayment(TEST_USER_ID, orderId, BigDecimal.valueOf(10000), BigDecimal.ZERO);
		assignPaymentId(payment);
		when(paymentRepository.findByOrderId(orderId)).thenReturn(Optional.of(payment));
		when(orderPort.validateOrder(orderId, 10000)).thenReturn(true);
		when(paymentConfirmHandler.onSuccess(payment.getId(), "pay-key"))
			.thenReturn(PaymentResponseDto.from(payment));

		PaymentResponseDto response = paymentService.confirm(
			TEST_USER_ID,
			new ConfirmPaymentRequestDto(orderId, "pay-key", 10000)
		);

		assertEquals(orderId, response.orderId());
		verify(paymentGatewayPort).confirm("pay-key", orderId.toString(), 10000);
		verify(paymentConfirmHandler).onSuccess(payment.getId(), "pay-key");
	}

	@Test
	void confirm_PG승인실패면_paymentConfirmHandler_onFailure_호출() {
		UUID orderId = UUID.randomUUID();
		Payment payment = createPayment(TEST_USER_ID, orderId, BigDecimal.valueOf(10000), BigDecimal.ZERO);
		assignPaymentId(payment);
		when(paymentRepository.findByOrderId(orderId)).thenReturn(Optional.of(payment));
		when(orderPort.validateOrder(orderId, 10000)).thenReturn(true);
		doThrow(new RuntimeException("pg error"))
			.when(paymentGatewayPort).confirm("pay-key", orderId.toString(), 10000);

		PaymentException exception = assertThrows(
			PaymentException.class,
			() -> paymentService.confirm(TEST_USER_ID, new ConfirmPaymentRequestDto(orderId, "pay-key", 10000))
		);

		assertEquals(PaymentErrorCode.PAYMENT_CONFIRM_FAILED, exception.getErrorCode());
		verify(paymentConfirmHandler).onFailure(payment.getId(), orderId, payment.getDepositAmount());
	}

	private PreparePaymentRequestDto prepareRequest(BigDecimal paymentAmount, BigDecimal depositAmount) {
		return new PreparePaymentRequestDto(
			UUID.randomUUID(),
			UUID.randomUUID(),
			UUID.randomUUID(),
			PaymentMethod.TOSS,
			paymentAmount,
			depositAmount
		);
	}

	private Payment createPayment(UUID userId, UUID orderId, BigDecimal paymentAmount, BigDecimal depositAmount) {
		return Payment.create(
			userId,
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
}
