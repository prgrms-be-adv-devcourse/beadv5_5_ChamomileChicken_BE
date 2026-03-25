package jabaclass.payment.application;

import jabaclass.payment.application.port.out.OrderPort;
import jabaclass.payment.application.port.out.PaymentGatewayPort;
import jabaclass.payment.application.service.PaymentService;
import jabaclass.payment.domain.model.Payment;
import jabaclass.payment.domain.model.PaymentMethod;
import jabaclass.payment.domain.repository.PaymentRepository;
import jabaclass.payment.presentation.dto.request.ConfirmPaymentRequestDto;
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
	private PaymentGatewayPort paymentGatewayPort;
	private OrderPort orderPort;

	private PaymentService paymentService;

	@BeforeEach
	void setUp() {
		paymentRepository = mock(PaymentRepository.class);
		paymentGatewayPort = mock(PaymentGatewayPort.class);
		orderPort = mock(OrderPort.class);

		paymentService = new PaymentService(
			paymentRepository,
			paymentGatewayPort,
			orderPort
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
			PaymentMethod.TOSS,
			BigDecimal.valueOf(30000),
			BigDecimal.ZERO
		);

		ReflectionTestUtils.setField(payment, "id", paymentId);

		when(paymentRepository.findById(paymentId))
			.thenReturn(Optional.of(payment));

		ConfirmPaymentRequestDto request = new ConfirmPaymentRequestDto(
			paymentId,
			"test_payment_key",
			30000
		);

		// when
		var result = paymentService.confirm(request);

		// then
		verify(paymentGatewayPort, times(1))
			.confirm(anyString(), anyString(), anyInt());

		// Order 호출 안 되는지 검증
		verify(orderPort, never()).updatePaymentStatus(any(), any(), any());

		assertThat(result).isNotNull();
		assertThat(payment.isDone()).isTrue();
		assertThat(payment.getPaymentKey()).isEqualTo("test_payment_key");
	}

	@Test
	void 결제_confirm_실패_payment가_없으면_예외() {
		// given
		UUID paymentId = UUID.randomUUID();

		when(paymentRepository.findById(paymentId))
			.thenReturn(Optional.empty());

		ConfirmPaymentRequestDto request =
			new ConfirmPaymentRequestDto(paymentId, "key", 30000);

		// when & then
		assertThatThrownBy(() -> paymentService.confirm(request))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("결제 정보를 찾을 수 없습니다.");
	}

	@Test
	void 결제_confirm_실패_금액이_다르면_예외() {
		// given
		UUID paymentId = UUID.randomUUID();

		Payment payment = Payment.create(
			UUID.randomUUID(),
			UUID.randomUUID(),
			UUID.randomUUID(),
			PaymentMethod.TOSS,
			BigDecimal.valueOf(30000),
			BigDecimal.ZERO
		);

		ReflectionTestUtils.setField(payment, "id", paymentId);

		when(paymentRepository.findById(paymentId))
			.thenReturn(Optional.of(payment));

		ConfirmPaymentRequestDto request =
			new ConfirmPaymentRequestDto(paymentId, "key", 10000);

		// when & then
		assertThatThrownBy(() -> paymentService.confirm(request))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("결제 금액이 일치하지 않습니다.");
	}

	@Test
	void 결제_confirm_이미_DONE이면_재처리하지_않는다() {
		// given
		UUID paymentId = UUID.randomUUID();

		Payment payment = Payment.create(
			UUID.randomUUID(),
			UUID.randomUUID(),
			UUID.randomUUID(),
			PaymentMethod.TOSS,
			BigDecimal.valueOf(30000),
			BigDecimal.ZERO
		);

		ReflectionTestUtils.setField(payment, "id", paymentId);

		payment.markDone("old_key");

		when(paymentRepository.findById(paymentId))
			.thenReturn(Optional.of(payment));

		ConfirmPaymentRequestDto request =
			new ConfirmPaymentRequestDto(paymentId, "new_key", 30000);

		// when
		var result = paymentService.confirm(request);

		// then
		verify(paymentGatewayPort, never()).confirm(any(), any(), anyInt());

		assertThat(result).isNotNull();
		assertThat(payment.getPaymentKey()).isEqualTo("old_key");
	}
}