package jabaclass.payment.domain;

import jabaclass.payment.common.exception.PaymentErrorCode;
import jabaclass.payment.common.exception.PaymentException;
import jabaclass.payment.domain.model.Payment;
import jabaclass.payment.domain.model.PaymentStatus;
import jabaclass.payment.domain.model.PaymentMethod;

import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

@SuppressWarnings("NonAsciiCharacters")
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class PaymentTest {

	@Test
	void 결제를_생성한다() {
		// given
		UUID userId = UUID.randomUUID();
		UUID productId = UUID.randomUUID();
		UUID orderId = UUID.randomUUID();

		BigDecimal paymentAmount = BigDecimal.valueOf(1000);
		BigDecimal depositAmount = BigDecimal.valueOf(500);

		// when
		Payment payment = Payment.create(
			userId,
			productId,
			orderId,
			UUID.randomUUID(),
			PaymentMethod.TOSS,
			paymentAmount,
			depositAmount
		);

		// then
		assertThat(payment.getUserId()).isEqualTo(userId);
		assertThat(payment.getTotalAmount())
			.isEqualByComparingTo(BigDecimal.valueOf(1500));
		assertThat(payment.getStatus()).isEqualTo(PaymentStatus.READY);
	}

	@Test
	void paymentAmount가_null이면_예외가_발생한다() {
		// given
		UUID userId = UUID.randomUUID();
		UUID productId = UUID.randomUUID();
		UUID orderId = UUID.randomUUID();

		// when & then
		assertThatThrownBy(() -> Payment.create(
			userId,
			productId,
			orderId,
			UUID.randomUUID(),
			PaymentMethod.TOSS,
			null,
			BigDecimal.valueOf(500)
		))
			.isInstanceOf(PaymentException.class)
			.hasMessage(PaymentErrorCode.INVALID_AMOUNT.getMessage());
	}

	@Test
	void depositAmount가_null이면_예외가_발생한다() {
		// given
		UUID userId = UUID.randomUUID();
		UUID productId = UUID.randomUUID();
		UUID orderId = UUID.randomUUID();

		// when & then
		assertThatThrownBy(() -> Payment.create(
			userId,
			productId,
			orderId,
			UUID.randomUUID(),
			PaymentMethod.TOSS,
			BigDecimal.valueOf(1000),
			null
		))
			.isInstanceOf(PaymentException.class)
			.hasMessage(PaymentErrorCode.INVALID_AMOUNT.getMessage());
	}

	@Test
	void paymentAmount가_0보다_작으면_예외가_발생한다() {
		// given
		UUID userId = UUID.randomUUID();
		UUID productId = UUID.randomUUID();
		UUID orderId = UUID.randomUUID();

		// when & then
		assertThatThrownBy(() -> Payment.create(
			userId,
			productId,
			orderId,
			UUID.randomUUID(),
			PaymentMethod.TOSS,
			BigDecimal.valueOf(-1),
			BigDecimal.valueOf(500)
		))
			.isInstanceOf(PaymentException.class)
			.hasMessage(PaymentErrorCode.INVALID_NEGATIVE_AMOUNT.getMessage());
	}

	@Test
	void depositAmount가_0보다_작으면_예외가_발생한다() {
		// given
		UUID userId = UUID.randomUUID();
		UUID productId = UUID.randomUUID();
		UUID orderId = UUID.randomUUID();

		// when & then
		assertThatThrownBy(() -> Payment.create(
			userId,
			productId,
			orderId,
			UUID.randomUUID(),
			PaymentMethod.TOSS,
			BigDecimal.valueOf(1000),
			BigDecimal.valueOf(-1)
		))
			.isInstanceOf(PaymentException.class)
			.hasMessage(PaymentErrorCode.INVALID_NEGATIVE_AMOUNT.getMessage());
	}

	@Test
	void productUserId가_null이면_예외가_발생한다() {
		assertThatThrownBy(() -> Payment.create(
			UUID.randomUUID(),
			UUID.randomUUID(),
			UUID.randomUUID(),
			null,
			PaymentMethod.TOSS,
			BigDecimal.valueOf(1000),
			BigDecimal.ZERO
		))
			.isInstanceOf(PaymentException.class)
			.hasMessage(PaymentErrorCode.INVALID_PRODUCT_USER.getMessage());
	}
}
