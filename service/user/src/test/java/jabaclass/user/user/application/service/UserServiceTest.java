package jabaclass.user.user.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

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
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;

import jabaclass.user.common.error.BusinessException;
import jabaclass.user.mail.application.usecase.EmailVerificationUseCase;
import jabaclass.user.user.application.exception.UserErrorCode;
import jabaclass.user.user.domain.model.User;
import jabaclass.user.user.domain.model.UserRole;
import jabaclass.user.user.domain.repository.UserRepository;
import jabaclass.user.user.presentation.dto.request.ChangeMyEmailRequestDto;
import jabaclass.user.user.presentation.dto.request.RegisterUserRequestDto;
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

//	@Test
//	void 회원가입을_성공한다() {
//		// given
//		RegisterUserRequestDto request = new RegisterUserRequestDto(
//			"용구",
//			"new@example.com",
//			"password123!",
//			"010-1234-5678",
//			"verified-token"
//		);
//
//		given(userRepository.existsByEmail(request.email()))
//			.willReturn(false);
//		given(userRepository.saveAndFlush(any(User.class)))
//			.willAnswer(invocation -> invocation.getArgument(0));
//
//		// when
//		userService.register(request);
//
//		// then
//		then(userRepository).should(times(1)).existsByEmail(request.email());
//		then(emailVerificationUseCase).should(times(1))
//			.validateVerifiedToken(request.email(), request.verifiedToken());
//		then(userRepository).should(times(1)).saveAndFlush(any(User.class));
//		then(userRepository).should(never()).save(any(User.class));
//	}

//	@Test
//	void 회원가입시_DB_유니크_제약조건_위반이면_이메일중복예외로_변환한다() {
//		// given
//		RegisterUserRequestDto request = new RegisterUserRequestDto(
//			"용구",
//			"duplicate@example.com",
//			"password123!",
//			"010-1234-5678",
//			"verified-token"
//		);
//
//		given(userRepository.existsByEmail(request.email()))
//			.willReturn(false);
//		given(userRepository.saveAndFlush(any(User.class)))
//			.willThrow(new DataIntegrityViolationException("uk_users_email"));
//
//		// when & then
//		assertThatThrownBy(() -> userService.register(request))
//			.isInstanceOf(BusinessException.class)
//			.hasMessage(UserErrorCode.EMAIL_ALREADY_EXISTS.getMessage());
//
//		then(userRepository).should(times(1)).existsByEmail(request.email());
//		then(emailVerificationUseCase).should(times(1))
//			.validateVerifiedToken(request.email(), request.verifiedToken());
//		then(userRepository).should(times(1)).saveAndFlush(any(User.class));
//	}

//	@Test
//	void 이메일이_중복되지_않으면_예외가_발생하지_않는다() {
//		// given
//		String email = "new@example.com";
//
//		given(userRepository.existsByEmail(email))
//			.willReturn(false);
//
//		// when
//		userService.checkEmailDuplicate(email);
//
//		// then
//		then(userRepository).should(times(1)).existsByEmail(email);
//	}

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

	@Test
	void 사용자_ID_목록으로_사용자_정보를_조회한다() {
		// given
		UUID secondUserId = UUID.randomUUID();

		User secondUser = User.builder()
			.name("철수")
			.email("chul@example.com")
			.password("encoded-password")
			.phone("010-9999-8888")
			.role(UserRole.USER)
			.deposit(BigDecimal.TEN)
			.build();

		ReflectionTestUtils.setField(secondUser, "id", secondUserId);

		List<UUID> userIds = List.of(userId, secondUserId);

		given(userRepository.findAllByIds(userIds))
			.willReturn(List.of(user, secondUser));

		// when
		List<UserResponseDto> result = userService.getUsersByIds(userIds);

		// then
		assertThat(result).hasSize(2);
		assertThat(result.get(0).userId()).isEqualTo(userId);
		assertThat(result.get(1).userId()).isEqualTo(secondUserId);

		then(userRepository).should(times(1)).findAllByIds(userIds);
	}

	@Test
	void 사용자_ID_목록이_비어있으면_빈_리스트를_반환한다() {
		// given
		List<UUID> userIds = List.of();

		// when
		List<UserResponseDto> result = userService.getUsersByIds(userIds);

		// then
		assertThat(result).isEmpty();

		then(userRepository).should(never()).findAllByIds(anyList());
	}

	@Test
	void 사용자_ID_목록에_중복이_있으면_중복을_제거하고_조회한다() {
		// given
		UUID secondUserId = UUID.randomUUID();

		User secondUser = User.builder()
			.name("철수")
			.email("chul@example.com")
			.password("encoded-password")
			.phone("010-9999-8888")
			.role(UserRole.ADMIN)
			.deposit(new BigDecimal("5000"))
			.build();

		ReflectionTestUtils.setField(secondUser, "id", secondUserId);

		List<UUID> userIds = List.of(userId, secondUserId, userId, secondUserId);

		given(userRepository.findAllByIds(List.of(userId, secondUserId)))
			.willReturn(List.of(secondUser, user));

		// when
		List<UserResponseDto> result = userService.getUsersByIds(userIds);

		// then
		assertThat(result).hasSize(2);

		assertThat(result.get(0).userId()).isEqualTo(userId);
		assertThat(result.get(0).name()).isEqualTo("용구");

		assertThat(result.get(1).userId()).isEqualTo(secondUserId);
		assertThat(result.get(1).name()).isEqualTo("철수");

		then(userRepository).should(times(1)).findAllByIds(List.of(userId, secondUserId));
	}

}