package jabaclass.user.deposit.application.usecase;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import jabaclass.user.common.error.BusinessException;
import jabaclass.user.deposit.domain.DepositType;
import jabaclass.user.deposit.domain.repository.DepositHistoryRepository;
import jabaclass.user.user.domain.model.User;
import jabaclass.user.user.domain.model.UserRole;
import jabaclass.user.user.domain.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class DepositChargeUseCaseTest {

	@Mock
	private UserRepository userRepository;

	@Mock
	private DepositHistoryRepository depositHistoryRepository;

	@InjectMocks
	private DepositChargeUseCase depositChargeUseCase;

	private UUID userId;
	private User user;

	@BeforeEach
	void setUp() {
		userId = UUID.randomUUID();

		user = User.builder()
			.name("홍길동")
			.email("test@test.com")
			.password("password123")
			.phone("010-1234-5678")
			.role(UserRole.USER)
			.build();

		ReflectionTestUtils.setField(user, "id", userId);
	}

	@Test
	void 예치금_증가_성공() {
		// given
		BigDecimal amount = new BigDecimal("10000");
		UUID paymentId = UUID.randomUUID();

		given(userRepository.findById(userId)).willReturn(Optional.of(user));
		given(depositHistoryRepository.save(any()))
			.willAnswer(invocation -> invocation.getArgument(0));

		// when
		depositChargeUseCase.increase(userId, amount, paymentId);

		// then
		assertThat(user.getDeposit()).isEqualByComparingTo(amount);

		then(depositHistoryRepository).should().save(argThat(history ->
			history.getUser().equals(user) &&
				history.getAmount().compareTo(amount) == 0 &&
				history.getType() == DepositType.CHARGE
		));
	}

	@Test
	void 존재하지_않는_유저_예외() {
		// given
		BigDecimal amount = new BigDecimal("10000");

		given(userRepository.findById(userId)).willReturn(Optional.empty());

		// when & then
		assertThatThrownBy(() ->
			depositChargeUseCase.increase(userId, amount, UUID.randomUUID())
		)
			.isInstanceOf(BusinessException.class)
			.hasMessage("존재하지 않는 회원입니다.");
	}
}