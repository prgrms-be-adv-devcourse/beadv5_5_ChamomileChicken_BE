package jabaclass.user.deposit.application.usecase;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
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
import jabaclass.user.deposit.application.DepositHistoryStatusUpdater;
import jabaclass.user.deposit.domain.DepositHistory;
import jabaclass.user.deposit.domain.DepositType;
import jabaclass.user.deposit.domain.repository.DepositHistoryRepository;
import jabaclass.user.deposit.infrastructure.client.PaymentClient;
import jabaclass.user.deposit.presentation.dto.request.DepositChargeRequestDto;
import jabaclass.user.deposit.presentation.dto.response.DepositChargeResponseDto;
import jabaclass.user.user.domain.model.User;
import jabaclass.user.user.domain.model.UserRole;
import jabaclass.user.user.domain.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class DepositChargeUseCaseTest {

	@Mock
	private UserRepository userRepository;

	@Mock
	private DepositHistoryRepository depositHistoryRepository;

	@Mock
	private PaymentClient paymentClient;

	@Mock
	private DepositHistoryStatusUpdater depositHistoryStatusUpdater;

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
	void 예치금_충전_성공() {
		// given
		UUID paymentId = UUID.randomUUID();
		BigDecimal chargeAmount = new BigDecimal("10000");
		DepositChargeRequestDto request = new DepositChargeRequestDto("CARD", chargeAmount);

		given(userRepository.findById(userId)).willReturn(Optional.of(user));
		given(paymentClient.createPayment(userId, chargeAmount, "CARD")).willReturn(paymentId);
		given(depositHistoryRepository.save(any(DepositHistory.class)))
			.willAnswer(invocation -> invocation.getArgument(0));

		// when
		DepositChargeResponseDto result = depositChargeUseCase.charge(userId, request);

		// then
		assertThat(result.depositPaymentId()).isEqualTo(paymentId);
		assertThat(result.chargeAmount()).isEqualByComparingTo(chargeAmount);
		assertThat(result.balance()).isEqualByComparingTo(chargeAmount);
		assertThat(result.success()).isTrue();
		assertThat(user.getDeposit()).isEqualByComparingTo(chargeAmount);
		then(depositHistoryRepository).should().save(argThat(history ->
			history.getUser().equals(user)
				&& history.getAmount().compareTo(chargeAmount) == 0
				&& history.getType() == DepositType.CHARGE
		));
		then(depositHistoryStatusUpdater).shouldHaveNoInteractions();
	}

	@Test
	void 존재하지_않는_유저로_충전시_예외_발생() {
		// given
		DepositChargeRequestDto request = new DepositChargeRequestDto("CARD", new BigDecimal("10000"));
		given(userRepository.findById(userId)).willReturn(Optional.empty());

		// when & then
		assertThatThrownBy(() -> depositChargeUseCase.charge(userId, request))
			.isInstanceOf(BusinessException.class)
			.hasMessage("존재하지 않는 회원입니다.");

		then(paymentClient).shouldHaveNoInteractions();
		then(depositHistoryRepository).shouldHaveNoInteractions();
		then(depositHistoryStatusUpdater).shouldHaveNoInteractions();
	}

	@Test
	void 결제_승인_실패시_예외가_발생하고_이력은_FAILED_상태로_업데이트된다() {
		// given
		BigDecimal chargeAmount = new BigDecimal("10000");
		DepositChargeRequestDto request = new DepositChargeRequestDto("CARD", chargeAmount);

		given(userRepository.findById(userId)).willReturn(Optional.of(user));
		given(depositHistoryRepository.save(any(DepositHistory.class)))
			.willAnswer(invocation -> invocation.getArgument(0));
		given(paymentClient.createPayment(userId, chargeAmount, "CARD"))
			.willThrow(new RuntimeException("결제 서비스 오류"));

		// when & then
		assertThatThrownBy(() -> depositChargeUseCase.charge(userId, request))
			.isInstanceOf(BusinessException.class)
			.hasMessage("결제에 실패했습니다.");

		then(depositHistoryStatusUpdater).should().updateFailed(any(DepositHistory.class));
	}
}