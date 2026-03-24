package jabaclass.user.deposit.application.usecase;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import jabaclass.user.deposit.domain.DepositHistory;
import jabaclass.user.deposit.domain.DepositType;
import jabaclass.user.deposit.domain.repository.DepositHistoryRepository;
import jabaclass.user.deposit.presentation.dto.response.DepositDetailResponseDto;
import jabaclass.user.deposit.presentation.dto.response.DepositHistoryResponseDto;
import jabaclass.user.deposit.presentation.dto.response.DepositMeResponseDto;
import jabaclass.user.user.domain.model.User;
import jabaclass.user.user.domain.model.UserRole;
import jabaclass.user.user.domain.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class DepositQueryUseCaseTest {

	@Mock
	private UserRepository userRepository;

	@Mock
	private DepositHistoryRepository depositHistoryRepository;

	@InjectMocks
	private DepositQueryUseCase depositQueryUseCase;

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

	// ========================
	// findMyDeposit
	// ========================

	@Test
	void 현재_예치금_잔액_정상_조회() {
		// given
		user.chargeDeposit(new BigDecimal("50000"));
		given(userRepository.findById(userId)).willReturn(Optional.of(user));

		// when
		DepositMeResponseDto result = depositQueryUseCase.findMyDeposit(userId);

		// then
		assertThat(result.userId()).isEqualTo(userId);
		assertThat(result.balance()).isEqualByComparingTo(new BigDecimal("50000"));
	}

	@Test
	void 존재하지_않는_유저로_예치금_조회시_예외() {
		// given
		given(userRepository.findById(userId)).willReturn(Optional.empty());

		// when & then
		assertThatThrownBy(() -> depositQueryUseCase.findMyDeposit(userId))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("존재하지 않는 회원입니다.");
	}

	// ========================
	// findAllDepositHistories
	// ========================

	@Test
	void 예치금_이력_목록_조회() {
		// given
		UUID paymentId1 = UUID.randomUUID();
		UUID paymentId2 = UUID.randomUUID();

		DepositHistory history1 = DepositHistory.of(user, paymentId1, new BigDecimal("10000"), DepositType.CHARGE);
		DepositHistory history2 = DepositHistory.of(user, paymentId2, new BigDecimal("5000"), DepositType.PAYMENT);
		ReflectionTestUtils.setField(history1, "id", UUID.randomUUID());
		ReflectionTestUtils.setField(history2, "id", UUID.randomUUID());

		given(depositHistoryRepository.findAllByUserId(userId)).willReturn(List.of(history1, history2));

		// when
		DepositHistoryResponseDto result = depositQueryUseCase.findAllDepositHistories(userId);

		// then
		assertThat(result.items()).hasSize(2);
		assertThat(result.items().get(0).paymentId()).isEqualTo(paymentId1);
		assertThat(result.items().get(0).type()).isEqualTo(DepositType.CHARGE);
		assertThat(result.items().get(1).paymentId()).isEqualTo(paymentId2);
		assertThat(result.items().get(1).type()).isEqualTo(DepositType.PAYMENT);
	}

	@Test
	void 예치금_이력이_없으면_빈_목록_반환() {
		// given
		given(depositHistoryRepository.findAllByUserId(userId)).willReturn(List.of());

		// when
		DepositHistoryResponseDto result = depositQueryUseCase.findAllDepositHistories(userId);

		// then
		assertThat(result.items()).isEmpty();
	}

	// ========================
	// findDepositHistory
	// ========================

	@Test
	void 예치금_상세정보_조회_성공() {
		// given
		UUID depositHistoryId = UUID.randomUUID();
		UUID paymentId = UUID.randomUUID();
		DepositHistory history = DepositHistory.of(user, paymentId, new BigDecimal("20000"), DepositType.CHARGE);
		ReflectionTestUtils.setField(history, "id", depositHistoryId);

		given(depositHistoryRepository.findById(depositHistoryId)).willReturn(Optional.of(history));

		// when
		DepositDetailResponseDto result = depositQueryUseCase.findDepositHistory(depositHistoryId);

		// then
		assertThat(result.depositHistoryId()).isEqualTo(depositHistoryId);
		assertThat(result.userId()).isEqualTo(userId);
		assertThat(result.paymentId()).isEqualTo(paymentId);
		assertThat(result.type()).isEqualTo(DepositType.CHARGE);
		assertThat(result.amount()).isEqualByComparingTo(new BigDecimal("20000"));
	}

	@Test
	void 존재하지_않는_예치금_이력_조회시_예외가_발생한다() {
		// given
		UUID depositHistoryId = UUID.randomUUID();
		given(depositHistoryRepository.findById(depositHistoryId)).willReturn(Optional.empty());

		// when & then
		assertThatThrownBy(() -> depositQueryUseCase.findDepositHistory(depositHistoryId))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("존재하지 않는 예치금 이력입니다.");
	}
}
