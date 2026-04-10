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

import jabaclass.payment.application.port.external.OrderPort;
import jabaclass.payment.application.port.external.PaymentGatewayPort;
import jabaclass.payment.common.exception.PaymentException;
import jabaclass.payment.domain.model.Payment;
import jabaclass.payment.domain.repository.PaymentRepository;
import jabaclass.payment.infrastructure.kafka.PaymentCompletedEventPublisher;
import jabaclass.payment.infrastructure.kafka.PaymentFailedEventPublisher;
import jabaclass.payment.presentation.dto.request.ConfirmPaymentRequestDto;
import jabaclass.payment.presentation.dto.request.PreparePaymentRequestDto;
import jabaclass.payment.application.service.PaymentService;

@ExtendWith(MockitoExtension.class)
public class PaymentTest {

	@Mock
	private PaymentRepository paymentRepository;

	@Mock
	private PaymentCompletedEventPublisher paymentCompletedEventPublisher;

	@Mock
	private PaymentFailedEventPublisher paymentFailedEventPublisher;

	@Mock
	private OrderPort orderPort;

	@Mock
	private PaymentGatewayPort paymentGatewayPort;

	@InjectMocks
	private PaymentService paymentService;

	@Test
	void create_예치금100퍼면_즉시완료_이벤트발행() {
		// given
		PreparePaymentRequestDto request = mock(PreparePaymentRequestDto.class);

		when(request.userId()).thenReturn(UUID.randomUUID());
		when(request.productId()).thenReturn(UUID.randomUUID());
		when(request.orderId()).thenReturn(UUID.randomUUID());
		when(request.productUserId()).thenReturn(UUID.randomUUID());
		when(request.paymentMethod()).thenReturn(null);
		when(request.paymentAmount()).thenReturn(BigDecimal.ZERO);
		when(request.depositAmount()).thenReturn(BigDecimal.valueOf(10000));

		when(paymentRepository.save(any()))
			.thenAnswer(invocation -> invocation.getArgument(0));

		// when
		paymentService.create(request);

		// then
		verify(paymentCompletedEventPublisher).publish(any());
	}

	@Test
	void create_일반결제면_이벤트발행안함() {
		// given
		PreparePaymentRequestDto request = mock(PreparePaymentRequestDto.class);

		when(request.userId()).thenReturn(UUID.randomUUID());
		when(request.productId()).thenReturn(UUID.randomUUID());
		when(request.orderId()).thenReturn(UUID.randomUUID());
		when(request.productUserId()).thenReturn(UUID.randomUUID());
		when(request.paymentMethod()).thenReturn(null);
		when(request.paymentAmount()).thenReturn(BigDecimal.valueOf(10000));
		when(request.depositAmount()).thenReturn(BigDecimal.ZERO);

		when(paymentRepository.save(any()))
			.thenAnswer(invocation -> invocation.getArgument(0));

		// when
		paymentService.create(request);

		// then
		verify(paymentCompletedEventPublisher, never()).publish(any());
	}

	@Test
	void confirm_성공하면_이벤트발행() {
		// given
		UUID orderId = UUID.randomUUID();

		Payment payment = mock(Payment.class);

		when(paymentRepository.findByOrderId(orderId))
			.thenReturn(Optional.of(payment));

		when(payment.isDone()).thenReturn(false);
		when(payment.getOrderId()).thenReturn(orderId);
		when(payment.getTotalAmount()).thenReturn(BigDecimal.valueOf(10000));
		when(payment.getPaymentAmount()).thenReturn(BigDecimal.valueOf(10000));
		when(payment.getId()).thenReturn(UUID.randomUUID());

		when(orderPort.validateOrder(orderId, 10000))
			.thenReturn(true);

		ConfirmPaymentRequestDto request =
			new ConfirmPaymentRequestDto(orderId, "key", 10000);

		// when
		paymentService.confirm(request);

		// then
		verify(paymentGatewayPort).confirm(any(), any(), anyInt());
		verify(payment).markDone("key");
		verify(paymentCompletedEventPublisher).publish(any());
	}

	@Test
	void confirm_이미완료면_아무것도안함() {
		// given
		UUID orderId = UUID.randomUUID();

		Payment payment = mock(Payment.class);

		when(paymentRepository.findByOrderId(orderId))
			.thenReturn(Optional.of(payment));

		when(payment.isDone()).thenReturn(true);

		ConfirmPaymentRequestDto request =
			new ConfirmPaymentRequestDto(orderId, "key", 10000);

		// when
		paymentService.confirm(request);

		// then
		verify(paymentGatewayPort, never()).confirm(any(), any(), anyInt());
		verify(paymentCompletedEventPublisher, never()).publish(any());
	}

	@Test
	void confirm_Order검증실패() {
		// given
		UUID orderId = UUID.randomUUID();

		Payment payment = mock(Payment.class);

		when(paymentRepository.findByOrderId(orderId))
			.thenReturn(Optional.of(payment));

		when(payment.isDone()).thenReturn(false);
		when(payment.getOrderId()).thenReturn(orderId);
		when(payment.getTotalAmount()).thenReturn(BigDecimal.valueOf(10000));

		when(orderPort.validateOrder(orderId, 10000))
			.thenReturn(false);

		ConfirmPaymentRequestDto request =
			new ConfirmPaymentRequestDto(orderId, "key", 10000);

		// when & then
		assertThrows(PaymentException.class,
			() -> paymentService.confirm(request));
	}

	@Test
	void confirm_PG실패시_failed이벤트발행() {
		// given
		UUID orderId = UUID.randomUUID();

		Payment payment = mock(Payment.class);

		when(paymentRepository.findByOrderId(orderId))
			.thenReturn(Optional.of(payment));

		when(payment.isDone()).thenReturn(false);
		when(payment.getOrderId()).thenReturn(orderId);
		when(payment.getTotalAmount()).thenReturn(BigDecimal.valueOf(10000));
		when(payment.getPaymentAmount()).thenReturn(BigDecimal.valueOf(10000));
		when(payment.getId()).thenReturn(UUID.randomUUID());

		when(orderPort.validateOrder(orderId, 10000))
			.thenReturn(true);

		doThrow(new RuntimeException())
			.when(paymentGatewayPort).confirm(any(), any(), anyInt());

		ConfirmPaymentRequestDto request =
			new ConfirmPaymentRequestDto(orderId, "key", 10000);

		// when
		assertThrows(PaymentException.class,
			() -> paymentService.confirm(request));

		// then
		verify(payment).markFailed();
		verify(paymentFailedEventPublisher).publish(any());
	}


}
