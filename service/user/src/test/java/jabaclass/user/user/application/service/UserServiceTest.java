package jabaclass.user.user.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

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
import jabaclass.user.mail.application.usecase.EmailVerificationUseCase;
import jabaclass.user.user.application.exception.UserErrorCode;
import jabaclass.user.user.domain.model.User;
import jabaclass.user.user.domain.model.UserRole;
import jabaclass.user.user.domain.repository.UserRepository;
import jabaclass.user.user.presentation.dto.request.ChangeMyEmailRequestDto;
import jabaclass.user.user.presentation.dto.request.UpdateUserRequestDto;
import jabaclass.user.user.presentation.dto.response.UserResponseDto;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

	@InjectMocks
	private UserService userService;

	@Mock
	private UserRepository userRepository;

	@Mock
	private EmailVerificationUseCase emailVerificationUseCase;

	private UUID userId;
	private User user;

	@BeforeEach
	void setUp() {
		userId = UUID.randomUUID();

		user = User.builder()
			.name("용구")
			.email("old@example.com")
			.password("encoded-password")
			.phone("010-1234-5678")
			.role(UserRole.USER)
			.deposit(BigDecimal.ZERO)
			.build();

		ReflectionTestUtils.setField(user, "id", userId);
	}

	@Test
	void 이메일이_중복되지_않으면_예외가_발생하지_않는다() {
		// given
		String email = "new@example.com";

		given(userRepository.existsByEmail(email))
			.willReturn(false);

		// when
		userService.checkEmailDuplicate(email);

		// then
		then(userRepository).should(times(1)).existsByEmail(email);
	}

	@Test
	void 이메일이_중복되면_예외가_발생한다() {
		// given
		String email = "duplicate@example.com";

		given(userRepository.existsByEmail(email))
			.willReturn(true);

		// when & then
		assertThatThrownBy(() -> userService.checkEmailDuplicate(email))
			.isInstanceOf(BusinessException.class)
			.hasMessage(UserErrorCode.EMAIL_ALREADY_EXISTS.getMessage());

		then(userRepository).should(times(1)).existsByEmail(email);
	}

	@Test
	void 내_정보를_조회한다() {
		// given
		given(userRepository.findById(userId))
			.willReturn(Optional.of(user));

		// when
		UserResponseDto result = userService.getMyInfo(userId);

		// then
		assertThat(result.userId()).isEqualTo(userId);
		assertThat(result.name()).isEqualTo("용구");
		assertThat(result.email()).isEqualTo("old@example.com");
		assertThat(result.phone()).isEqualTo("010-1234-5678");
		assertThat(result.role()).isEqualTo(UserRole.USER);
		assertThat(result.deposit()).isEqualByComparingTo(BigDecimal.ZERO);

		then(userRepository).should(times(1)).findById(userId);
	}

	@Test
	void 내_정보_조회시_사용자가_없으면_예외가_발생한다() {
		// given
		given(userRepository.findById(userId))
			.willReturn(Optional.empty());

		// when & then
		assertThatThrownBy(() -> userService.getMyInfo(userId))
			.isInstanceOf(BusinessException.class)
			.hasMessage(UserErrorCode.USER_NOT_FOUND.getMessage());

		then(userRepository).should(times(1)).findById(userId);
	}

	@Test
	void 내_정보를_수정한다() {
		// given
		UpdateUserRequestDto request = new UpdateUserRequestDto("새이름", "010-9999-0000");

		given(userRepository.findById(userId))
			.willReturn(Optional.of(user));

		// when
		userService.updateMyInfo(userId, request);

		// then
		assertThat(user.getName()).isEqualTo("새이름");
		assertThat(user.getPhone()).isEqualTo("010-9999-0000");

		then(userRepository).should(times(1)).findById(userId);
	}

	@Test
	void 내_정보_수정시_사용자가_없으면_예외가_발생한다() {
		// given
		UpdateUserRequestDto request = new UpdateUserRequestDto("새이름", "010-9999-0000");

		given(userRepository.findById(userId))
			.willReturn(Optional.empty());

		// when & then
		assertThatThrownBy(() -> userService.updateMyInfo(userId, request))
			.isInstanceOf(BusinessException.class)
			.hasMessage(UserErrorCode.USER_NOT_FOUND.getMessage());

		then(userRepository).should(times(1)).findById(userId);
	}

	@Test
	void 이메일을_변경한다() {
		// given
		ChangeMyEmailRequestDto request = new ChangeMyEmailRequestDto(
			"new@example.com",
			"verified-token"
		);

		given(userRepository.existsByEmail(request.newEmail()))
			.willReturn(false);
		given(userRepository.findById(userId))
			.willReturn(Optional.of(user));

		// when
		userService.changeEmail(userId, request);

		// then
		assertThat(user.getEmail()).isEqualTo("new@example.com");

		then(userRepository).should(times(1)).existsByEmail(request.newEmail());
		then(emailVerificationUseCase).should(times(1))
			.validateVerifiedToken(request.newEmail(), request.verifiedToken());
		then(userRepository).should(times(1)).findById(userId);
	}

	@Test
	void 이메일_변경시_중복된_이메일이면_예외가_발생한다() {
		// given
		ChangeMyEmailRequestDto request = new ChangeMyEmailRequestDto(
			"duplicate@example.com",
			"verified-token"
		);

		given(userRepository.existsByEmail(request.newEmail()))
			.willReturn(true);

		// when & then
		assertThatThrownBy(() -> userService.changeEmail(userId, request))
			.isInstanceOf(BusinessException.class)
			.hasMessage(UserErrorCode.EMAIL_ALREADY_EXISTS.getMessage());

		then(userRepository).should(times(1)).existsByEmail(request.newEmail());
		then(emailVerificationUseCase).should(never()).validateVerifiedToken(anyString(), anyString());
		then(userRepository).should(never()).findById(any());
	}

	@Test
	void 이메일_변경시_사용자가_없으면_예외가_발생한다() {
		// given
		ChangeMyEmailRequestDto request = new ChangeMyEmailRequestDto(
			"new@example.com",
			"verified-token"
		);

		given(userRepository.existsByEmail(request.newEmail()))
			.willReturn(false);
		given(userRepository.findById(userId))
			.willReturn(Optional.empty());

		// when & then
		assertThatThrownBy(() -> userService.changeEmail(userId, request))
			.isInstanceOf(BusinessException.class)
			.hasMessage(UserErrorCode.USER_NOT_FOUND.getMessage());

		then(userRepository).should(times(1)).existsByEmail(request.newEmail());
		then(emailVerificationUseCase).should(times(1))
			.validateVerifiedToken(request.newEmail(), request.verifiedToken());
		then(userRepository).should(times(1)).findById(userId);
	}

	@Test
	void 회원을_탈퇴시킨다() {
		// given
		given(userRepository.findById(userId))
			.willReturn(Optional.of(user));

		// when
		userService.withdraw(userId);

		// then
		then(userRepository).should(times(1)).findById(userId);
		then(userRepository).should(times(1)).delete(user);
	}

	@Test
	void 회원_탈퇴시_사용자가_없으면_예외가_발생한다() {
		// given
		given(userRepository.findById(userId))
			.willReturn(Optional.empty());

		// when & then
		assertThatThrownBy(() -> userService.withdraw(userId))
			.isInstanceOf(BusinessException.class)
			.hasMessage(UserErrorCode.USER_NOT_FOUND.getMessage());

		then(userRepository).should(times(1)).findById(userId);
		then(userRepository).should(never()).delete(any());
	}
}