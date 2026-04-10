package jabaclass.payment;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import jabaclass.payment.application.port.external.PaymentGatewayPort;
import jabaclass.payment.application.service.PaymentService;
import jabaclass.payment.common.exception.PaymentException;
import jabaclass.payment.domain.model.Payment;
import jabaclass.payment.domain.repository.PaymentRepository;
import jabaclass.payment.infrastructure.kafka.PaymentRefundedEventPublisher;
import jabaclass.payment.presentation.dto.request.RefundPaymentRequestDto;

@ExtendWith(MockitoExtension.class)
class PaymentRefundTest {

	@Mock
	private PaymentRepository paymentRepository;

	@Mock
	private PaymentRefundedEventPublisher paymentRefundedEventPublisher;

	@Mock
	private PaymentGatewayPort paymentGatewayPort;

	@InjectMocks
	private PaymentService paymentService;

	@Test
	void 환불_성공_PG호출_이벤트발행() {
		// given
		UUID orderId = UUID.randomUUID();
		Payment payment = mock(Payment.class);

		when(paymentRepository.findByOrderId(orderId))
			.thenReturn(Optional.of(payment));

		when(payment.isDone()).thenReturn(true);
		when(payment.getPaymentAmount()).thenReturn(BigDecimal.valueOf(10000));
		when(payment.getDepositAmount()).thenReturn(BigDecimal.ZERO);
		when(payment.getPaymentKey()).thenReturn("key");
		when(payment.getId()).thenReturn(UUID.randomUUID());
		when(payment.getOrderId()).thenReturn(orderId);

		// when
		paymentService.refund(new RefundPaymentRequestDto(orderId));

		// then
		verify(paymentGatewayPort)
			.refund(eq("key"), eq(10000));
		verify(payment).markCancelled();
		verify(paymentRefundedEventPublisher).publish(any());
	}

	@Test
	void 환불_성공_예치금만이면_PG호출안함() {
		// given
		UUID orderId = UUID.randomUUID();
		Payment payment = mock(Payment.class);

		when(paymentRepository.findByOrderId(orderId))
			.thenReturn(Optional.of(payment));

		when(payment.isDone()).thenReturn(true);
		when(payment.getPaymentAmount()).thenReturn(BigDecimal.ZERO);
		when(payment.getDepositAmount()).thenReturn(BigDecimal.valueOf(10000));
		when(payment.getId()).thenReturn(UUID.randomUUID());
		when(payment.getOrderId()).thenReturn(orderId);

		// when
		paymentService.refund(new RefundPaymentRequestDto(orderId));

		// then
		verify(paymentGatewayPort, never()).refund(anyString(), anyInt());
		verify(payment).markCancelled();
		verify(paymentRefundedEventPublisher).publish(any());
	}

	@Test
	void 환불_실패_payment없으면_예외() {
		// given
		UUID orderId = UUID.randomUUID();

		when(paymentRepository.findByOrderId(orderId))
			.thenReturn(Optional.empty());

		// when & then
		assertThrows(PaymentException.class,
			() -> paymentService.refund(new RefundPaymentRequestDto(orderId))
		);

		verify(paymentRefundedEventPublisher, never()).publish(any());
	}

	@Test
	void 환불_PG실패시_이벤트발행안함() {
		// given
		UUID orderId = UUID.randomUUID();
		Payment payment = mock(Payment.class);

		when(paymentRepository.findByOrderId(orderId))
			.thenReturn(Optional.of(payment));

		when(payment.isDone()).thenReturn(true);
		when(payment.getPaymentAmount()).thenReturn(BigDecimal.valueOf(10000));
		when(payment.getDepositAmount()).thenReturn(BigDecimal.ZERO);
		when(payment.getPaymentKey()).thenReturn("key");
		when(payment.getId()).thenReturn(UUID.randomUUID());
		when(payment.getOrderId()).thenReturn(orderId);

		doThrow(new RuntimeException())
			.when(paymentGatewayPort).refund(anyString(), anyInt());

		// when & then
		assertThrows(PaymentException.class,
			() -> paymentService.refund(new RefundPaymentRequestDto(orderId))
		);

		verify(paymentRefundedEventPublisher, never()).publish(any());
	}
}