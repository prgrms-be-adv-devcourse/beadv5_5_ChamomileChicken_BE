package jabaclass.payment.application;

import jabaclass.payment.application.port.external.OrderPort;
import jabaclass.payment.application.port.external.PaymentGatewayPort;
import jabaclass.payment.application.port.external.UserPort;
import jabaclass.payment.application.service.PaymentService;
import jabaclass.payment.common.exception.PaymentErrorCode;
import jabaclass.payment.common.exception.PaymentException;
import jabaclass.payment.domain.model.Payment;
import jabaclass.payment.domain.model.PaymentMethod;
import jabaclass.payment.domain.model.Refund;
import jabaclass.payment.domain.model.RefundStatus;
import jabaclass.payment.domain.repository.PaymentRepository;
import jabaclass.payment.domain.repository.RefundRepository;
import jabaclass.payment.presentation.dto.request.ConfirmPaymentRequestDto;
import jabaclass.payment.presentation.dto.request.RefundPaymentRequestDto;
import jabaclass.payment.infrastructure.kafka.PaymentRefundCompletedEventPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class PaymentServiceTest {

	private PaymentRepository paymentRepository;
	private RefundRepository refundRepository;
	private PaymentGatewayPort paymentGatewayPort;
	private OrderPort orderPort;
	private UserPort userPort;
	private PaymentRefundCompletedEventPublisher refundCompletedEventPublisher;

	private PaymentService paymentService;

	@BeforeEach
	void setUp() {
		paymentRepository = mock(PaymentRepository.class);
		refundRepository = mock(RefundRepository.class);
		paymentGatewayPort = mock(PaymentGatewayPort.class);
		orderPort = mock(OrderPort.class);
		userPort = mock(UserPort.class);
		refundCompletedEventPublisher = mock(PaymentRefundCompletedEventPublisher.class);

		paymentService = new PaymentService(
			paymentRepository,
			refundRepository,
			paymentGatewayPort,
			orderPort,
			userPort,
			refundCompletedEventPublisher
		);
	}

	@Test
	void 결제_confirm_성공() {
		// given
		UUID paymentId = UUID.randomUUID();
		UUID orderId = UUID.randomUUID();

		Payment payment = Payment.create(
			UUID.randomUUID(),
			UUID.randomUUID(),
			orderId,
			UUID.randomUUID(),
			PaymentMethod.TOSS,
			BigDecimal.valueOf(30000),
			BigDecimal.ZERO
		);

		ReflectionTestUtils.setField(payment, "id", paymentId);

		when(paymentRepository.findByOrderId(orderId))
			.thenReturn(Optional.of(payment));

		when(orderPort.validateOrder(orderId, 30000))
			.thenReturn(true);

		ConfirmPaymentRequestDto request =
			new ConfirmPaymentRequestDto(orderId, "test_payment_key", 30000);

		// when
		var result = paymentService.confirm(request);

		// then
		verify(paymentGatewayPort, times(1))
			.confirm(anyString(), anyString(), anyInt());

		verify(orderPort, times(1))
			.updatePaymentStatus(
				eq(orderId),
				eq(paymentId),
				eq(0),
				eq("PAID")
			);

		assertThat(result).isNotNull();
		assertThat(payment.isDone()).isTrue();
		assertThat(payment.getPaymentKey()).isEqualTo("test_payment_key");
	}

	@Test
	void 결제_confirm_실패_payment가_없으면_예외() {
		// given
		UUID orderId = UUID.randomUUID();

		when(paymentRepository.findByOrderId(orderId))
			.thenReturn(Optional.empty());

		ConfirmPaymentRequestDto request =
			new ConfirmPaymentRequestDto(orderId, "key", 30000);

		// when & then
		assertThatThrownBy(() -> paymentService.confirm(request))
			.isInstanceOf(PaymentException.class)
			.hasMessage(PaymentErrorCode.PAYMENT_NOT_FOUND.getMessage());
	}

	@Test
	void 결제_confirm_실패_Order_금액_검증_실패() {
		// given
		UUID paymentId = UUID.randomUUID();
		UUID orderId = UUID.randomUUID();

		Payment payment = Payment.create(
			UUID.randomUUID(),
			UUID.randomUUID(),
			orderId,
			UUID.randomUUID(),
			PaymentMethod.TOSS,
			BigDecimal.valueOf(30000),
			BigDecimal.ZERO
		);

		ReflectionTestUtils.setField(payment, "id", paymentId);

		when(paymentRepository.findByOrderId(orderId))
			.thenReturn(Optional.of(payment));

		when(orderPort.validateOrder(orderId, 30000))
			.thenReturn(false);

		ConfirmPaymentRequestDto request =
			new ConfirmPaymentRequestDto(orderId, "key", 30000);

		// when & then
		assertThatThrownBy(() -> paymentService.confirm(request))
			.isInstanceOf(PaymentException.class)
			.hasMessage(PaymentErrorCode.INVALID_ORDER_AMOUNT.getMessage());

		verify(paymentGatewayPort, never()).confirm(any(), any(), anyInt());
		verify(orderPort, never())
			.updatePaymentStatus(any(), any(), anyInt(), any());
	}

	@Test
	void 결제_confirm_실패_Payment_금액_불일치() {
		// given
		UUID paymentId = UUID.randomUUID();
		UUID orderId = UUID.randomUUID();

		Payment payment = Payment.create(
			UUID.randomUUID(),
			UUID.randomUUID(),
			orderId,
			UUID.randomUUID(),
			PaymentMethod.TOSS,
			BigDecimal.valueOf(30000),
			BigDecimal.ZERO
		);

		ReflectionTestUtils.setField(payment, "id", paymentId);

		when(paymentRepository.findByOrderId(orderId))
			.thenReturn(Optional.of(payment));

		when(orderPort.validateOrder(orderId, 30000))
			.thenReturn(true);

		ConfirmPaymentRequestDto request =
			new ConfirmPaymentRequestDto(orderId, "key", 10000);

		// when & then
		assertThatThrownBy(() -> paymentService.confirm(request))
			.isInstanceOf(PaymentException.class)
			.hasMessage(PaymentErrorCode.INVALID_PAYMENT_AMOUNT.getMessage());

		verify(paymentGatewayPort, never()).confirm(any(), any(), anyInt());
		verify(orderPort, never())
			.updatePaymentStatus(any(), any(), anyInt(), any());
	}

	@Test
	void 결제_confirm_이미_DONE이면_재처리하지_않는다() {
		// given
		UUID paymentId = UUID.randomUUID();
		UUID orderId = UUID.randomUUID();

		Payment payment = Payment.create(
			UUID.randomUUID(),
			UUID.randomUUID(),
			orderId,
			UUID.randomUUID(),
			PaymentMethod.TOSS,
			BigDecimal.valueOf(30000),
			BigDecimal.ZERO
		);

		ReflectionTestUtils.setField(payment, "id", paymentId);

		payment.markDone("old_key");

		when(paymentRepository.findByOrderId(orderId))
			.thenReturn(Optional.of(payment));

		ConfirmPaymentRequestDto request =
			new ConfirmPaymentRequestDto(orderId, "new_key", 30000);

		// when
		var result = paymentService.confirm(request);

		// then
		verify(paymentGatewayPort, never()).confirm(any(), any(), anyInt());
		verify(orderPort, never())
			.updatePaymentStatus(any(), any(), anyInt(), any());

		assertThat(result).isNotNull();
		assertThat(payment.getPaymentKey()).isEqualTo("old_key");
	}

	@Test
	void 환불_성공_예치금과_toss환불을_처리한다() {
		UUID orderId = UUID.randomUUID();
		UUID paymentId = UUID.randomUUID();
		UUID productUserId = UUID.randomUUID();

		Payment payment = Payment.create(
			UUID.randomUUID(),
			UUID.randomUUID(),
			orderId,
			productUserId,
			PaymentMethod.TOSS,
			BigDecimal.valueOf(25000),
			BigDecimal.valueOf(5000)
		);
		ReflectionTestUtils.setField(payment, "id", paymentId);
		ReflectionTestUtils.setField(payment, "paymentKey", "pay_key");
		ReflectionTestUtils.setField(payment, "status", jabaclass.payment.domain.model.PaymentStatus.PAID);

		when(paymentRepository.findByOrderId(orderId)).thenReturn(Optional.of(payment));
		when(refundRepository.save(any(Refund.class))).thenAnswer(invocation -> invocation.getArgument(0));

		var response = paymentService.refund(new RefundPaymentRequestDto(orderId));

		verify(paymentGatewayPort).refund("pay_key", 25000);
		verify(userPort).refundDeposit(eq(payment.getUserId()), eq(BigDecimal.valueOf(5000)), any(UUID.class));
		verify(refundCompletedEventPublisher).publish(any());
		assertThat(response.orderId()).isEqualTo(orderId);
		assertThat(response.refundStatus()).isEqualTo(RefundStatus.COMPLETED);
	}

	@Test
	void 환불_성공_예치금_전액결제면_toss호출을_생략한다() {
		UUID orderId = UUID.randomUUID();
		UUID productUserId = UUID.randomUUID();

		Payment payment = Payment.create(
			UUID.randomUUID(),
			UUID.randomUUID(),
			orderId,
			productUserId,
			PaymentMethod.TOSS,
			BigDecimal.ZERO,
			BigDecimal.valueOf(10000)
		);
		ReflectionTestUtils.setField(payment, "paymentKey", "unused");
		ReflectionTestUtils.setField(payment, "status", jabaclass.payment.domain.model.PaymentStatus.PAID);

		when(paymentRepository.findByOrderId(orderId)).thenReturn(Optional.of(payment));
		when(refundRepository.save(any(Refund.class))).thenAnswer(invocation -> invocation.getArgument(0));

		paymentService.refund(new RefundPaymentRequestDto(orderId));

		verify(paymentGatewayPort, never()).refund(anyString(), anyInt());
		verify(userPort).refundDeposit(eq(payment.getUserId()), eq(BigDecimal.valueOf(10000)), any(UUID.class));
		verify(refundCompletedEventPublisher).publish(any());
	}
}
