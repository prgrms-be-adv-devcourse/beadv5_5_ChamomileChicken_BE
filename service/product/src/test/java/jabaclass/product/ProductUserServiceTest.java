package jabaclass.product;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

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

import jabaclass.product.application.acl.SellerRepository;
import jabaclass.product.application.exception.BusinessException;
import jabaclass.product.application.service.ProductUserService;
import jabaclass.product.common.exception.CommonErrorCode;
import jabaclass.product.domain.model.ProductUser;
import jabaclass.product.domain.model.status.ReservationStatus;
import jabaclass.product.domain.repository.ProductUserRepository;
import jabaclass.product.infrastructure.acl.dto.response.UserResponseDto;
import jabaclass.product.presentation.dto.request.CreateProductUserRequestDto;
import jabaclass.product.presentation.dto.response.ProductUserResponseDto;

@ExtendWith(MockitoExtension.class)
class ProductUserServiceTest {

	@InjectMocks
	private ProductUserService productUserService;

	@Mock
	private ProductUserRepository pUserRepository;

	@Mock
	private SellerRepository sellerRepository;

	private static final UUID USER_ID = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
	private static final UUID SCHEDULE_ID = UUID.fromString("223e4567-e89b-12d3-a456-426614174000");
	private static final UUID PRODUCT_USER_ID = UUID.fromString("323e4567-e89b-12d3-a456-426614174000");

	private ProductUser productUser;

	@BeforeEach
	void setUp() {
		productUser = ProductUser.builder()
			.productScheduleId(SCHEDULE_ID)
			.userId(USER_ID)
			.guestCount(2)
			.status(ReservationStatus.CONFIRMED)
			.build();
		ReflectionTestUtils.setField(productUser, "id", PRODUCT_USER_ID);
	}

	@Test
	void 예약자_목록을_조회한다() {
		given(pUserRepository.findByProductScheduleId(SCHEDULE_ID)).willReturn(List.of(productUser));
		given(sellerRepository.findSellerList(List.of(USER_ID)))
			.willReturn(Optional.of(List.of(new UserResponseDto(USER_ID, "buyer", "USER"))));

		List<ProductUserResponseDto> result = productUserService.getUser(SCHEDULE_ID);

		assertThat(result).hasSize(1);
		assertThat(result.get(0).id()).isEqualTo(PRODUCT_USER_ID);
		assertThat(result.get(0).productScheduleId()).isEqualTo(SCHEDULE_ID);
		assertThat(result.get(0).userName()).isEqualTo("buyer");
		assertThat(result.get(0).guestCount()).isEqualTo(2);
	}

	@Test
	void 예약자를_생성한다() {
		CreateProductUserRequestDto request = new CreateProductUserRequestDto(
			SCHEDULE_ID,
			USER_ID,
			3,
			ReservationStatus.RESERVED
		);
		given(sellerRepository.findSeller(USER_ID))
			.willReturn(Optional.of(new UserResponseDto(USER_ID, "buyer", "USER")));
		given(pUserRepository.save(any(ProductUser.class))).willAnswer(invocation -> {
			ProductUser saved = invocation.getArgument(0);
			ReflectionTestUtils.setField(saved, "id", PRODUCT_USER_ID);
			return saved;
		});

		ProductUserResponseDto result = productUserService.create(request);

		assertThat(result.id()).isEqualTo(PRODUCT_USER_ID);
		assertThat(result.productScheduleId()).isEqualTo(SCHEDULE_ID);
		assertThat(result.userName()).isEqualTo("buyer");
		assertThat(result.guestCount()).isEqualTo(3);
		assertThat(result.statusName()).isEqualTo(ReservationStatus.RESERVED.getStatusName());
	}

	@Test
	void 예약자_단건을_조회한다() {
		given(pUserRepository.findById(PRODUCT_USER_ID)).willReturn(Optional.of(productUser));

		ProductUserResponseDto result = productUserService.findById(PRODUCT_USER_ID);

		assertThat(result.id()).isEqualTo(PRODUCT_USER_ID);
		assertThat(result.productScheduleId()).isEqualTo(SCHEDULE_ID);
		assertThat(result.userName()).isNull();
	}

	@Test
	void 예약자_엔티티를_조회한다() {
		given(pUserRepository.findById(PRODUCT_USER_ID)).willReturn(Optional.of(productUser));

		ProductUser result = productUserService.innerFindById(PRODUCT_USER_ID);

		assertThat(result.getId()).isEqualTo(PRODUCT_USER_ID);
		assertThat(result.getProductScheduleId()).isEqualTo(SCHEDULE_ID);
	}

	@Test
	void 스케줄별_예약자_엔티티_목록을_조회한다() {
		given(pUserRepository.findByProductScheduleId(SCHEDULE_ID)).willReturn(List.of(productUser));

		List<ProductUser> result = productUserService.innserUserList(SCHEDULE_ID);

		assertThat(result).hasSize(1);
		assertThat(result.get(0).getId()).isEqualTo(PRODUCT_USER_ID);
	}

	@Test
	void 예약자_정보가_없으면_생성에_실패한다() {
		CreateProductUserRequestDto request = new CreateProductUserRequestDto(
			SCHEDULE_ID,
			USER_ID,
			3,
			ReservationStatus.RESERVED
		);
		given(sellerRepository.findSeller(USER_ID)).willReturn(Optional.empty());

		assertThatThrownBy(() -> productUserService.create(request))
			.isInstanceOf(BusinessException.class)
			.hasMessage(CommonErrorCode.SELLER_NOT_FOUND.getMessage());
	}
}
