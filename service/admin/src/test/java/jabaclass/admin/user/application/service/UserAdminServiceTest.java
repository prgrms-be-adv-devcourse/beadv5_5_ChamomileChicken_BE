package jabaclass.admin.user.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator.ReplaceUnderscores;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import jabaclass.admin.common.error.BusinessException;
import jabaclass.admin.user.domain.model.User;
import jabaclass.admin.user.domain.model.UserRole;
import jabaclass.admin.user.domain.repository.UserAdminRepository;
import jabaclass.admin.user.presentation.dto.response.UserAdminResponseDto;

@SuppressWarnings("NonAsciiCharacters")
@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(ReplaceUnderscores.class)
class UserAdminServiceTest {

	@Mock
	private UserAdminRepository userAdminRepository;

	@InjectMocks
	private UserAdminService userAdminService;

	private UUID userId;
	private User user;

	@BeforeEach
	void setUp() {
		userId = UUID.randomUUID();
		user = User.builder()
			.name("테스트유저")
			.email("test@example.com")
			.phone("010-1234-5678")
			.role(UserRole.USER)
			.build();
		ReflectionTestUtils.setField(user, "id", userId);
	}

	@Test
	void 전체_유저_목록을_조회한다() {
		// given
		Pageable pageable = PageRequest.of(0, 10);
		given(userAdminRepository.findAll(pageable))
			.willReturn(new PageImpl<>(List.of(user)));

		// when
		Page<UserAdminResponseDto> result = userAdminService.getUsers(pageable);

		// then
		assertThat(result.getContent()).hasSize(1);
		assertThat(result.getContent().get(0).id()).isEqualTo(userId);
		assertThat(result.getContent().get(0).email()).isEqualTo("test@example.com");
		then(userAdminRepository).should(times(1)).findAll(pageable);
	}

	@Test
	void 특정_유저를_조회한다() {
		// given
		given(userAdminRepository.findById(userId)).willReturn(Optional.of(user));

		// when
		UserAdminResponseDto result = userAdminService.getUser(userId);

		// then
		assertThat(result.id()).isEqualTo(userId);
		assertThat(result.name()).isEqualTo("테스트유저");
		assertThat(result.role()).isEqualTo(UserRole.USER);
		then(userAdminRepository).should(times(1)).findById(userId);
	}

	@Test
	void 존재하지_않는_유저_조회시_예외가_발생한다() {
		// given
		given(userAdminRepository.findById(userId)).willReturn(Optional.empty());

		// when & then
		assertThatThrownBy(() -> userAdminService.getUser(userId))
			.isInstanceOf(BusinessException.class)
			.hasMessage("유저를 찾을 수 없습니다.");
		then(userAdminRepository).should(times(1)).findById(userId);
	}

	@Test
	void 셀러를_승인한다() {
		// given
		given(userAdminRepository.findById(userId)).willReturn(Optional.of(user));

		// when
		userAdminService.approveSeller(userId);

		// then
		assertThat(user.getRole()).isEqualTo(UserRole.SELLER);
		then(userAdminRepository).should(times(1)).findById(userId);
	}

	@Test
	void 존재하지_않는_유저_셀러_승인시_예외가_발생한다() {
		// given
		given(userAdminRepository.findById(userId)).willReturn(Optional.empty());

		// when & then
		assertThatThrownBy(() -> userAdminService.approveSeller(userId))
			.isInstanceOf(BusinessException.class)
			.hasMessage("유저를 찾을 수 없습니다.");
		then(userAdminRepository).should(times(1)).findById(userId);
	}
}
