package jabaclass.product;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

import jabaclass.product.application.acl.SellerRepository;
import jabaclass.product.application.exception.BusinessException;
import jabaclass.product.application.service.AuditorAwareService;
import jabaclass.product.application.service.ScheduleService;
import jabaclass.product.application.usecase.ProductUseCase;
import jabaclass.product.application.usecase.ProductUserUseCase;
import jabaclass.product.common.exception.CommonErrorCode;
import jabaclass.product.domain.model.Product;
import jabaclass.product.domain.model.ProductUser;
import jabaclass.product.domain.model.Schedule;
import jabaclass.product.domain.model.status.ProductStatus;
import jabaclass.product.domain.model.status.ReservationStatus;
import jabaclass.product.domain.model.status.ReservedStatus;
import jabaclass.product.domain.repository.ScheduleRepository;
import jabaclass.product.infrastructure.acl.dto.response.UserResponseDto;
import jabaclass.product.presentation.dto.request.CreateProductUserRequestDto;
import jabaclass.product.presentation.dto.request.CreateScheduleRequestDto;
import jabaclass.product.presentation.dto.request.OrderRequestDto;
import jabaclass.product.presentation.dto.request.UpdateScheduleRequestDto;
import jabaclass.product.presentation.dto.respose.AvailabilityScheduleResponseDto;
import jabaclass.product.presentation.dto.respose.DeleteScheduleResposeDto;
import jabaclass.product.presentation.dto.respose.OrderResponseDto;
import jabaclass.product.presentation.dto.respose.ProductUserResponseDto;
import jabaclass.product.presentation.dto.respose.SchedulesResponseDto;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;

@ExtendWith(MockitoExtension.class)
class ScheduleTest {

	@InjectMocks
	private ScheduleService scheduleService;

	@Mock
	private ScheduleRepository scheduleRepository;

	@Mock
	private ProductUseCase productUseCase;

	@Mock
	private ProductUserUseCase productUserUseCase;

	@Mock
	private SellerRepository sellerRepository;

	@Mock
	private ApplicationEventPublisher publisher;

	@Mock
	private AuditorAwareService auditorAwareService;

	private static final UUID SELLER_ID = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
	private static final UUID PRODUCT_ID = UUID.fromString("223e4567-e89b-12d3-a456-426614174000");
	private static final UUID SCHEDULE_ID = UUID.fromString("323e4567-e89b-12d3-a456-426614174000");
	private static final UUID USER_ID = UUID.fromString("423e4567-e89b-12d3-a456-426614174000");
	private static final UUID PRODUCT_USER_ID = UUID.fromString("523e4567-e89b-12d3-a456-426614174000");
	private static final BigDecimal PRICE = new BigDecimal("1000.50");

	private Validator validator;
	private Product product;
	private Schedule schedule;
	private CreateScheduleRequestDto createRequest;
	private UpdateScheduleRequestDto updateRequest;

	@BeforeEach
	void setUp() {
		validator = Validation.buildDefaultValidatorFactory().getValidator();

		product = Product.builder()
			.id(PRODUCT_ID)
			.sellerId(SELLER_ID)
			.title("product-A")
			.maxCapacity(10)
			.description("test product")
			.price(PRICE)
			.status(ProductStatus.ENABLE)
			.build();

		schedule = Schedule.builder()
			.productId(PRODUCT_ID)
			.scheduleDt(LocalDate.now().plusDays(1))
			.startTime(LocalTime.of(10, 0))
			.endTime(LocalTime.of(12, 0))
			.status(ReservedStatus.AVAILABLE)
			.maxCapacity(10)
			.build();
		ReflectionTestUtils.setField(schedule, "id", SCHEDULE_ID);

		createRequest = new CreateScheduleRequestDto(
			LocalDate.now().plusDays(1).toString(),
			"10:00",
			"12:00",
			ReservedStatus.AVAILABLE
		);

		updateRequest = new UpdateScheduleRequestDto(
			"13:00",
			"14:00",
			ReservedStatus.CLOSED,
			5
		);
	}

	@Test
	void 일정_생성에_성공한다() {
		권한있는_판매자와_본인상품을_준비한다();
		given(scheduleRepository.findConflictSchedules(
			eq(PRODUCT_ID),
			eq(LocalDate.parse(createRequest.scheduleDt())),
			eq(LocalTime.parse(createRequest.startTime())),
			eq(LocalTime.parse(createRequest.endTime()))
		)).willReturn(Collections.emptyList());
		given(scheduleRepository.save(any(Schedule.class)))
			.willAnswer(invocation -> {
				Schedule saved = invocation.getArgument(0);
				ReflectionTestUtils.setField(saved, "id", SCHEDULE_ID);
				return saved;
			});

		SchedulesResponseDto result = scheduleService.create(createRequest, PRODUCT_ID);

		assertThat(result.id()).isEqualTo(SCHEDULE_ID);
		assertThat(result.productId()).isEqualTo(PRODUCT_ID);
		assertThat(result.maxCapacity()).isEqualTo(10);
	}

	@Test
	void 존재하지_않는_날짜면_일정_생성에_실패한다() {
		CreateScheduleRequestDto request = new CreateScheduleRequestDto("2026-02-30", "10:00", "12:00",
			ReservedStatus.AVAILABLE);
		권한있는_판매자와_본인상품을_준비한다();

		비즈니스_예외를_검증한다(() -> scheduleService.create(request, PRODUCT_ID), HttpStatus.BAD_REQUEST,
			CommonErrorCode.DATE_BAD_FORMAT);
	}

	@Test
	void 날짜_형식이_올바르지_않으면_일정_생성에_실패한다() {
		CreateScheduleRequestDto request = new CreateScheduleRequestDto("2026/03/30", "10:00", "12:00",
			ReservedStatus.AVAILABLE);
		권한있는_판매자와_본인상품을_준비한다();

		비즈니스_예외를_검증한다(() -> scheduleService.create(request, PRODUCT_ID), HttpStatus.BAD_REQUEST,
			CommonErrorCode.DATE_BAD_FORMAT);
	}

	@Test
	void 시간_형식이_올바르지_않으면_일정_생성에_실패한다() {
		CreateScheduleRequestDto request = new CreateScheduleRequestDto(
			LocalDate.now().plusDays(1).toString(),
			"25:61",
			"12:00",
			ReservedStatus.AVAILABLE
		);
		권한있는_판매자와_본인상품을_준비한다();

		비즈니스_예외를_검증한다(() -> scheduleService.create(request, PRODUCT_ID), HttpStatus.BAD_REQUEST,
			CommonErrorCode.TIME_BAD_FORMAT);
	}

	@Test
	void 겹치는_일정이_있으면_일정_생성에_실패한다() {
		권한있는_판매자와_본인상품을_준비한다();
		given(scheduleRepository.findConflictSchedules(
			eq(PRODUCT_ID),
			eq(LocalDate.parse(createRequest.scheduleDt())),
			eq(LocalTime.parse(createRequest.startTime())),
			eq(LocalTime.parse(createRequest.endTime()))
		)).willReturn(List.of(schedule));

		비즈니스_예외를_검증한다(() -> scheduleService.create(createRequest, PRODUCT_ID), HttpStatus.BAD_REQUEST,
			CommonErrorCode.SCHEDULE_CONFLICT);
	}

	@Test
	void 일정_수정에_성공한다() {
		권한있는_판매자와_본인상품을_준비한다();
		given(scheduleRepository.findByIdAndDeleteDtIsNull(SCHEDULE_ID)).willReturn(Optional.of(schedule));
		given(scheduleRepository.findConflictSchedulesNoId(
			eq(PRODUCT_ID),
			eq(schedule.getScheduleDt()),
			eq(LocalTime.parse(updateRequest.startTime())),
			eq(LocalTime.parse(updateRequest.endTime())),
			eq(SCHEDULE_ID)
		)).willReturn(Collections.emptyList());

		SchedulesResponseDto result = scheduleService.update(updateRequest, PRODUCT_ID, SCHEDULE_ID);

		assertThat(result.id()).isEqualTo(SCHEDULE_ID);
		assertThat(result.startTime()).isEqualTo(LocalTime.of(13, 0));
		assertThat(result.endTime()).isEqualTo(LocalTime.of(14, 0));
		assertThat(result.maxCapacity()).isEqualTo(5);
	}

	@Test
	void 수정할_일정이_없으면_일정_수정에_실패한다() {
		권한있는_판매자와_본인상품을_준비한다();
		given(scheduleRepository.findByIdAndDeleteDtIsNull(SCHEDULE_ID)).willReturn(Optional.empty());

		비즈니스_예외를_검증한다(() -> scheduleService.update(updateRequest, PRODUCT_ID, SCHEDULE_ID), HttpStatus.NOT_FOUND,
			CommonErrorCode.SCHDULES_NOT_FOUND);
	}

	@Test
	void 수정_시간_형식이_올바르지_않으면_일정_수정에_실패한다() {
		UpdateScheduleRequestDto request = new UpdateScheduleRequestDto("99:99", "14:00", ReservedStatus.AVAILABLE, 5);
		권한있는_판매자와_본인상품을_준비한다();
		given(scheduleRepository.findByIdAndDeleteDtIsNull(SCHEDULE_ID)).willReturn(Optional.of(schedule));

		비즈니스_예외를_검증한다(() -> scheduleService.update(request, PRODUCT_ID, SCHEDULE_ID), HttpStatus.BAD_REQUEST,
			CommonErrorCode.TIME_BAD_FORMAT);
	}

	@Test
	void 수정_시작시간이_종료시간과_같거나_늦으면_일정_수정에_실패한다() {
		UpdateScheduleRequestDto request = new UpdateScheduleRequestDto("14:00", "14:00", ReservedStatus.AVAILABLE, 5);
		권한있는_판매자와_본인상품을_준비한다();
		given(scheduleRepository.findByIdAndDeleteDtIsNull(SCHEDULE_ID)).willReturn(Optional.of(schedule));

		비즈니스_예외를_검증한다(() -> scheduleService.update(request, PRODUCT_ID, SCHEDULE_ID), HttpStatus.BAD_REQUEST,
			CommonErrorCode.INVALID_TIME_RANGE);
	}

	@Test
	void 겹치는_수정_일정이_있으면_일정_수정에_실패한다() {
		권한있는_판매자와_본인상품을_준비한다();
		given(scheduleRepository.findByIdAndDeleteDtIsNull(SCHEDULE_ID)).willReturn(Optional.of(schedule));
		given(scheduleRepository.findConflictSchedulesNoId(
			eq(PRODUCT_ID),
			eq(schedule.getScheduleDt()),
			eq(LocalTime.parse(updateRequest.startTime())),
			eq(LocalTime.parse(updateRequest.endTime())),
			eq(SCHEDULE_ID)
		)).willReturn(List.of(schedule));

		비즈니스_예외를_검증한다(() -> scheduleService.update(updateRequest, PRODUCT_ID, SCHEDULE_ID), HttpStatus.BAD_REQUEST,
			CommonErrorCode.SCHEDULE_CONFLICT);
	}

	@Test
	void 판매자_권한이_아니면_일정_수정에_실패한다() {
		given(auditorAwareService.getCurrentAuditor()).willReturn(Optional.of(SELLER_ID));
		given(sellerRepository.findSeller(SELLER_ID))
			.willReturn(Optional.of(new UserResponseDto(SELLER_ID, "user", "USER")));

		비즈니스_예외를_검증한다(() -> scheduleService.update(updateRequest, PRODUCT_ID, SCHEDULE_ID), HttpStatus.FORBIDDEN,
			CommonErrorCode.NOT_SELLER);
	}

	@Test
	void 상품이_존재하지_않으면_일정_수정에_실패한다() {
		given(auditorAwareService.getCurrentAuditor()).willReturn(Optional.of(SELLER_ID));
		given(sellerRepository.findSeller(SELLER_ID))
			.willReturn(Optional.of(new UserResponseDto(SELLER_ID, "seller", "SELLER")));
		given(productUseCase.findByIdOrThrow(PRODUCT_ID))
			.willThrow(new BusinessException(CommonErrorCode.PRODUCT_NOT_FOUND));

		비즈니스_예외를_검증한다(() -> scheduleService.update(updateRequest, PRODUCT_ID, SCHEDULE_ID), HttpStatus.NOT_FOUND,
			CommonErrorCode.PRODUCT_NOT_FOUND);
	}

	@Test
	void 수정_DTO에서_형식이_올바르지_않으면_검증에_실패한다() {
		UpdateScheduleRequestDto request = new UpdateScheduleRequestDto("9:00", "14-00", ReservedStatus.AVAILABLE, 0);

		Set<ConstraintViolation<UpdateScheduleRequestDto>> violations = validator.validate(request);

		assertThat(violations).extracting(ConstraintViolation::getPropertyPath)
			.anyMatch(path -> path.toString().equals("startTime"))
			.anyMatch(path -> path.toString().equals("endTime"))
			.anyMatch(path -> path.toString().equals("maxCapacity"));
	}

	@Test
	void 예약_검증에_성공하면_예약을_생성하고_OK를_반환한다() {
		OrderRequestDto request = new OrderRequestDto(SCHEDULE_ID, USER_ID, 3, PRICE);
		ProductUserResponseDto createdUser = new ProductUserResponseDto(
			PRODUCT_USER_ID,
			SCHEDULE_ID,
			"user",
			3,
			ReservationStatus.RESERVED.getStatusName()
		);
		given(scheduleRepository.findByIdAndDeleteDtIsNull(SCHEDULE_ID)).willReturn(Optional.of(schedule));
		given(scheduleRepository.verification(3, SCHEDULE_ID)).willReturn(1);
		given(productUserUseCase.create(any(CreateProductUserRequestDto.class))).willReturn(createdUser);
		given(productUseCase.findByIdOrThrow(PRODUCT_ID)).willReturn(product);

		OrderResponseDto result = scheduleService.verification(request);

		assertThat(result.valid()).isEqualTo(jabaclass.product.presentation.dto.respose.OrderValid.OK);
		assertThat(result.productUserId()).isEqualTo(PRODUCT_USER_ID);
		then(productUserUseCase).should().create(any(CreateProductUserRequestDto.class));
	}

	@Test
	void 예약_수량이_재고보다_많으면_OUT_OF_STOCK을_반환한다() {
		OrderRequestDto request = new OrderRequestDto(SCHEDULE_ID, USER_ID, 11, PRICE);
		given(scheduleRepository.findByIdAndDeleteDtIsNull(SCHEDULE_ID)).willReturn(Optional.of(schedule));
		given(scheduleRepository.verification(11, SCHEDULE_ID)).willReturn(0);
		given(productUseCase.findByIdOrThrow(PRODUCT_ID)).willReturn(product);

		OrderResponseDto result = scheduleService.verification(request);

		assertThat(result.valid()).isEqualTo(jabaclass.product.presentation.dto.respose.OrderValid.OUT_OF_STOCK);
		assertThat(result.productUserId()).isNull();
		then(productUserUseCase).should(never()).create(any(CreateProductUserRequestDto.class));
	}

	@Test
	void 예약_검증시_가격이_다르면_PRICE_MISMATCH를_반환한다() {
		OrderRequestDto request = new OrderRequestDto(SCHEDULE_ID, USER_ID, 7, new BigDecimal("9999"));
		given(scheduleRepository.findByIdAndDeleteDtIsNull(SCHEDULE_ID)).willReturn(Optional.of(schedule));
		given(productUseCase.findByIdOrThrow(PRODUCT_ID)).willReturn(product);

		OrderResponseDto result = scheduleService.verification(request);

		assertThat(result.valid()).isEqualTo(jabaclass.product.presentation.dto.respose.OrderValid.PRICE_MISMATCH);
		assertThat(result.productUserId()).isNull();
		then(productUserUseCase).should(never()).create(any(CreateProductUserRequestDto.class));
	}

	@Test
	void 예약_해제_요청이_오면_예약_상태를_RELEASED로_변경한다() {
		ProductUser productUser = ProductUser.builder()
			.productScheduleId(SCHEDULE_ID)
			.userId(USER_ID)
			.guestCount(2)
			.status(ReservationStatus.CONFIRMED)
			.build();
		ReflectionTestUtils.setField(productUser, "id", PRODUCT_USER_ID);
		given(productUserUseCase.innerFindById(PRODUCT_USER_ID)).willReturn(productUser);
		given(scheduleRepository.findByIdAndDeleteDtIsNull(SCHEDULE_ID)).willReturn(Optional.of(schedule));
		given(productUseCase.findByIdOrThrow(PRODUCT_ID)).willReturn(product);
		given(scheduleRepository.restoreCapacity(SCHEDULE_ID, 2, product.getMaxCapacity())).willReturn(1);
		given(productUserUseCase.innserUserList(SCHEDULE_ID)).willReturn(List.of(productUser));

		scheduleService.releaseReservation(PRODUCT_USER_ID);

		assertThat(productUser.getStatus()).isEqualTo(ReservationStatus.RELEASED);
	}

	@Test
	void 환불후_예약을_다시_계산해_full상태를_available로_복구한다() {
		ProductUser refundedUser = ProductUser.builder()
			.productScheduleId(SCHEDULE_ID)
			.userId(USER_ID)
			.guestCount(4)
			.status(ReservationStatus.CONFIRMED)
			.build();
		ProductUser activeUser = ProductUser.builder()
			.productScheduleId(SCHEDULE_ID)
			.userId(UUID.randomUUID())
			.guestCount(2)
			.status(ReservationStatus.CONFIRMED)
			.build();
		ReflectionTestUtils.setField(refundedUser, "id", PRODUCT_USER_ID);
		schedule.changeStatus(ReservedStatus.FULL);
		given(productUserUseCase.innerFindById(PRODUCT_USER_ID)).willReturn(refundedUser);
		given(scheduleRepository.findByIdAndDeleteDtIsNull(SCHEDULE_ID)).willReturn(Optional.of(schedule));
		given(productUseCase.findByIdOrThrow(PRODUCT_ID)).willReturn(product);
		given(scheduleRepository.restoreCapacity(SCHEDULE_ID, 4, product.getMaxCapacity())).willReturn(1);
		given(productUserUseCase.innserUserList(SCHEDULE_ID)).willReturn(List.of(refundedUser, activeUser));

		scheduleService.refundReservation(PRODUCT_USER_ID);

		assertThat(refundedUser.getStatus()).isEqualTo(ReservationStatus.REFUNDED);
		assertThat(schedule.getStatus()).isEqualTo(ReservedStatus.AVAILABLE);
	}

	@Test
	void 일정_삭제에_성공한다() {
		권한있는_판매자와_본인상품을_준비한다();
		given(scheduleRepository.findByIdAndDeleteDtIsNull(SCHEDULE_ID)).willReturn(Optional.of(schedule));

		DeleteScheduleResposeDto result = scheduleService.delete(PRODUCT_ID, SCHEDULE_ID);

		assertThat(result.scheduleId()).isEqualTo(SCHEDULE_ID);
		assertThat(result.status()).isEqualTo(ReservedStatus.CLOSED);
		assertThat(schedule.getDeleteDt()).isNotNull();
	}

	@Test
	void 상품의_일정_목록을_조회한다() {
		Schedule secondSchedule = Schedule.builder()
			.productId(PRODUCT_ID)
			.scheduleDt(LocalDate.now().plusDays(2))
			.startTime(LocalTime.of(14, 0))
			.endTime(LocalTime.of(16, 0))
			.status(ReservedStatus.AVAILABLE)
			.maxCapacity(8)
			.build();
		ReflectionTestUtils.setField(secondSchedule, "id", UUID.randomUUID());
		given(scheduleRepository.findByProductIdAndDeleteDtIsNull(PRODUCT_ID)).willReturn(List.of(schedule, secondSchedule));

		List<SchedulesResponseDto> result = scheduleService.schedulesList(PRODUCT_ID);

		assertThat(result).hasSize(2);
	}

	@Test
	void 일정별_예약_상태를_조회한다() {
		ProductUser paidUser = ProductUser.builder()
			.productScheduleId(SCHEDULE_ID)
			.userId(UUID.randomUUID())
			.guestCount(4)
			.status(ReservationStatus.CONFIRMED)
			.build();
		ProductUser pendingUser = ProductUser.builder()
			.productScheduleId(SCHEDULE_ID)
			.userId(UUID.randomUUID())
			.guestCount(2)
			.status(ReservationStatus.RESERVED)
			.build();
		given(scheduleRepository.findByIdAndDeleteDtIsNull(SCHEDULE_ID)).willReturn(Optional.of(schedule));
		given(productUserUseCase.innserUserList(SCHEDULE_ID)).willReturn(List.of(paidUser, pendingUser));

		AvailabilityScheduleResponseDto result = scheduleService.availabilitySchedule(SCHEDULE_ID);

		assertThat(result.scheduleId()).isEqualTo(SCHEDULE_ID);
		assertThat(result.reservedCount()).isEqualTo(6);
		assertThat(result.remainingCount()).isEqualTo(4);
	}

	private void 권한있는_판매자와_본인상품을_준비한다() {
		given(auditorAwareService.getCurrentAuditor()).willReturn(Optional.of(SELLER_ID));
		given(sellerRepository.findSeller(eq(SELLER_ID)))
			.willReturn(Optional.of(new UserResponseDto(SELLER_ID, "seller", "SELLER")));
		given(productUseCase.findByIdOrThrow(eq(PRODUCT_ID))).willReturn(product);
		lenient().when(productUseCase.matchProductAndSellerId(eq(PRODUCT_ID), eq(SELLER_ID))).thenReturn(product);
	}

	private void 비즈니스_예외를_검증한다(ThrowingCall call, HttpStatus status, CommonErrorCode errorCode) {
		assertThatThrownBy(call::invoke)
			.isInstanceOf(BusinessException.class)
			.hasMessage(errorCode.getMessage())
			.extracting(throwable -> ((BusinessException)throwable).getStatus())
			.isEqualTo(status);
	}

	@FunctionalInterface
	private interface ThrowingCall {
		void invoke();
	}
}
