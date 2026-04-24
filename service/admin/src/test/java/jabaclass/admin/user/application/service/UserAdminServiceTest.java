package jabaclass.admin.user.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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

import com.fasterxml.jackson.databind.ObjectMapper;

import jabaclass.admin.common.error.BusinessException;
import jabaclass.admin.product.domain.model.OutboxEvent;
import jabaclass.admin.product.domain.repository.OutboxEventRepository;
import jabaclass.admin.user.domain.dto.UserSearchCondition;
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

	@Mock
	private OutboxEventRepository outboxEventRepository;

	@Mock
	private ObjectMapper objectMapper;

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
	void 조건_없으면_전체_조회_성공() {
		// given
		Pageable pageable = PageRequest.of(0, 10);
		UserSearchCondition condition = new UserSearchCondition(null, null, null);
		given(userAdminRepository.findAll(any(UserSearchCondition.class), eq(pageable)))
			.willReturn(new PageImpl<>(List.of(user)));

		// when
		Page<UserAdminResponseDto> result = userAdminService.getUsers(pageable, condition);

		// then
		assertThat(result.getContent()).hasSize(1);
		assertThat(result.getContent().get(0).id()).isEqualTo(userId);
		assertThat(result.getContent().get(0).email()).isEqualTo("test@example.com");
		then(userAdminRepository).should(times(1)).findAll(any(UserSearchCondition.class), eq(pageable));
	}

	@Test
	void 역할_필터로_SELLER만_조회_성공() {
		// given
		Pageable pageable = PageRequest.of(0, 10);
		UserSearchCondition condition = new UserSearchCondition("SELLER", null, null);
		User seller = User.builder()
			.name("판매자")
			.email("seller@example.com")
			.phone("010-9999-8888")
			.role(UserRole.SELLER)
			.build();
		ReflectionTestUtils.setField(seller, "id", UUID.randomUUID());

		given(userAdminRepository.findAll(any(UserSearchCondition.class), eq(pageable)))
			.willReturn(new PageImpl<>(List.of(seller)));

		// when
		Page<UserAdminResponseDto> result = userAdminService.getUsers(pageable, condition);

		// then
		assertThat(result.getContent()).hasSize(1);
		assertThat(result.getContent().get(0).role()).isEqualTo(UserRole.SELLER);
		assertThat(result.getContent().get(0).name()).isEqualTo("판매자");
		then(userAdminRepository).should(times(1)).findAll(any(UserSearchCondition.class), eq(pageable));
	}

	@Test
	void 이름_검색으로_부분일치_조회_성공() {
		// given
		Pageable pageable = PageRequest.of(0, 10);
		UserSearchCondition condition = new UserSearchCondition(null, "테스트", null);
		given(userAdminRepository.findAll(any(UserSearchCondition.class), eq(pageable)))
			.willReturn(new PageImpl<>(List.of(user)));

		// when
		Page<UserAdminResponseDto> result = userAdminService.getUsers(pageable, condition);

		// then
		assertThat(result.getContent()).hasSize(1);
		assertThat(result.getContent().get(0).name()).contains("테스트");
		then(userAdminRepository).should(times(1)).findAll(any(UserSearchCondition.class), eq(pageable));
	}

	@Test
	void 이메일_검색으로_부분일치_조회_성공() {
		// given
		Pageable pageable = PageRequest.of(0, 10);
		UserSearchCondition condition = new UserSearchCondition(null, null, "test@");
		given(userAdminRepository.findAll(any(UserSearchCondition.class), eq(pageable)))
			.willReturn(new PageImpl<>(List.of(user)));

		// when
		Page<UserAdminResponseDto> result = userAdminService.getUsers(pageable, condition);

		// then
		assertThat(result.getContent()).hasSize(1);
		assertThat(result.getContent().get(0).email()).contains("test@");
		then(userAdminRepository).should(times(1)).findAll(any(UserSearchCondition.class), eq(pageable));
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
	void 셀러를_승인하면_아웃박스_이벤트를_저장한다() throws Exception {
		// given
		given(userAdminRepository.findById(userId)).willReturn(Optional.of(user));
		given(objectMapper.writeValueAsString(any())).willReturn("{\"type\":\"SELLER_APPROVED\"}");

		// when
		userAdminService.approveSeller(userId);

		// then
		assertThat(user.getRole()).isEqualTo(UserRole.SELLER);
		then(userAdminRepository).should(times(1)).findById(userId);
		then(outboxEventRepository).should(times(1)).save(any(OutboxEvent.class));
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
		then(outboxEventRepository).shouldHaveNoInteractions();
	}
}
