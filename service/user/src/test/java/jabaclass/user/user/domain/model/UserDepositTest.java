package jabaclass.user.user.domain.model;

import static org.assertj.core.api.Assertions.*;

import java.math.BigDecimal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class UserDepositTest {

	private User user;

	@BeforeEach
	void setUp() {
		user = User.builder()
			.name("홍길동")
			.email("test@test.com")
			.password("password123")
			.phone("010-1234-5678")
			.role(UserRole.USER)
			.build();
	}

	@Test
	void 초기_예치금은_0() {
		// given: setUp()에서 생성된 신규 유저

		// when: 아무 조작 없이 잔액 조회

		// then
		assertThat(user.getDeposit()).isEqualByComparingTo(BigDecimal.ZERO);
	}

	@Test
	void chargeDeposit_성공() {
		// given
		BigDecimal chargeAmount = new BigDecimal("10000");

		// when
		user.chargeDeposit(chargeAmount);

		// then
		assertThat(user.getDeposit()).isEqualByComparingTo(new BigDecimal("10000"));
	}

	@Test
	void chargeDeposit_누적충전() {
		// given
		user.chargeDeposit(new BigDecimal("10000"));

		// when
		user.chargeDeposit(new BigDecimal("5000"));

		// then
		assertThat(user.getDeposit()).isEqualByComparingTo(new BigDecimal("15000"));
	}

	@Test
	void deductDeposit_성공() {
		// given
		user.chargeDeposit(new BigDecimal("10000"));

		// when
		user.deductDeposit(new BigDecimal("3000"));

		// then
		assertThat(user.getDeposit()).isEqualByComparingTo(new BigDecimal("7000"));
	}

	@Test
	void deductDeposit_전액차감_성공() {
		// given
		user.chargeDeposit(new BigDecimal("10000"));

		// when
		user.deductDeposit(new BigDecimal("10000"));

		// then
		assertThat(user.getDeposit()).isEqualByComparingTo(BigDecimal.ZERO);
	}

	@Test
	void deductDeposit_잔액부족_예외() {
		// given
		user.chargeDeposit(new BigDecimal("5000"));

		// when & then
		assertThatThrownBy(() -> user.deductDeposit(new BigDecimal("10000")))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("예치금이 부족합니다.");
	}
}
