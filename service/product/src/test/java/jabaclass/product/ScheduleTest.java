package jabaclass.product;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

import jabaclass.product.application.acl.SellerRepository;
import jabaclass.product.application.exception.BusinessException;
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
import jabaclass.product.presentation.dto.request.CreateProductUserRequestDto;
import jabaclass.product.presentation.dto.request.CreateScheduleRequestDto;
import jabaclass.product.presentation.dto.request.OrderRequestDto;
import jabaclass.product.presentation.dto.request.UpdateScheduleRequestDto;
import jabaclass.product.presentation.dto.respose.AvailabilityScheduleResponseDto;
import jabaclass.product.presentation.dto.respose.DeleteScheduleResposeDto;
import jabaclass.product.presentation.dto.respose.OrderResponseDto;
import jabaclass.product.presentation.dto.respose.OrderValid;
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
			.title("test-product")
			.maxCapacity(10)
			.description("test-description")
			.price(PRICE)
			.status(ProductStatus.ENABLE)
			.build();

		schedule = Schedule.builder()
			.productId(PRODUCT_ID)
			.scheduleDt(LocalDate.now().plusDays(1))
			.startTime(LocalTime.of(10, 0))
			.endTime(LocalTime.of(12, 0))
			.status(ReservedStatus.AVAILABLE)
			.capacity(10)
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
		given(productUseCase.findByIdOrThrow(PRODUCT_ID)).willReturn(product);
		given(productUseCase.matchProductAndSellerId(PRODUCT_ID, SELLER_ID)).willReturn(product);
		given(scheduleRepository.findConflictSchedules(
			eq(PRODUCT_ID),
			eq(LocalDate.parse(createRequest.scheduleDt())),
			eq(LocalTime.parse(createRequest.startTime())),
			eq(LocalTime.parse(createRequest.endTime()))
		)).willReturn(List.of());
		given(scheduleRepository.save(any(Schedule.class))).willAnswer(invocation -> {
			Schedule saved = invocation.getArgument(0);
			ReflectionTestUtils.setField(saved, "id", SCHEDULE_ID);
			return saved;
		});

		SchedulesResponseDto result = scheduleService.create(createRequest, PRODUCT_ID, SELLER_ID);

		assertThat(result.id()).isEqualTo(SCHEDULE_ID);
		assertThat(result.productId()).isEqualTo(PRODUCT_ID);
		assertThat(result.capacity()).isEqualTo(10);
	}

	@Test
	void 과거_날짜면_일정_생성에_실패한다() {
		CreateScheduleRequestDto request = new CreateScheduleRequestDto(
			LocalDate.now().minusDays(1).toString(),
			"10:00",
			"12:00",
			ReservedStatus.AVAILABLE
		);
		given(productUseCase.findByIdOrThrow(PRODUCT_ID)).willReturn(product);
		given(productUseCase.matchProductAndSellerId(PRODUCT_ID, SELLER_ID)).willReturn(product);

		assertBusinessException(() -> scheduleService.create(request, PRODUCT_ID, SELLER_ID), HttpStatus.BAD_REQUEST,
			CommonErrorCode.INVALID_SCHEDULE_DATE);
	}

	@Test
	void 존재하지_않는_날짜면_일정_생성에_실패한다() {
		CreateScheduleRequestDto request = new CreateScheduleRequestDto("2026-02-30", "10:00", "12:00",
			ReservedStatus.AVAILABLE);
		given(productUseCase.findByIdOrThrow(PRODUCT_ID)).willReturn(product);
		given(productUseCase.matchProductAndSellerId(PRODUCT_ID, SELLER_ID)).willReturn(product);

		assertBusinessException(() -> scheduleService.create(request, PRODUCT_ID, SELLER_ID), HttpStatus.BAD_REQUEST,
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
		given(productUseCase.findByIdOrThrow(PRODUCT_ID)).willReturn(product);
		given(productUseCase.matchProductAndSellerId(PRODUCT_ID, SELLER_ID)).willReturn(product);

		assertBusinessException(() -> scheduleService.create(request, PRODUCT_ID, SELLER_ID), HttpStatus.BAD_REQUEST,
			CommonErrorCode.TIME_BAD_FORMAT);
	}

	@Test
	void 겹치는_일정이_있으면_일정_생성에_실패한다() {
		given(productUseCase.findByIdOrThrow(PRODUCT_ID)).willReturn(product);
		given(productUseCase.matchProductAndSellerId(PRODUCT_ID, SELLER_ID)).willReturn(product);
		given(scheduleRepository.findConflictSchedules(
			eq(PRODUCT_ID),
			eq(LocalDate.parse(createRequest.scheduleDt())),
			eq(LocalTime.parse(createRequest.startTime())),
			eq(LocalTime.parse(createRequest.endTime()))
		)).willReturn(List.of(schedule));

		assertBusinessException(() -> scheduleService.create(createRequest, PRODUCT_ID, SELLER_ID), HttpStatus.BAD_REQUEST,
			CommonErrorCode.SCHEDULE_CONFLICT);
	}

	@Test
	void 일정_수정에_성공한다() {
		given(productUseCase.findByIdOrThrow(PRODUCT_ID)).willReturn(product);
		given(productUseCase.matchProductAndSellerId(PRODUCT_ID, SELLER_ID)).willReturn(product);
		given(scheduleRepository.findByIdAndDeleteDtIsNull(SCHEDULE_ID)).willReturn(Optional.of(schedule));
		given(scheduleRepository.findConflictSchedulesNoId(
			eq(PRODUCT_ID),
			eq(schedule.getScheduleDt()),
			eq(LocalTime.parse(updateRequest.startTime())),
			eq(LocalTime.parse(updateRequest.endTime())),
			eq(SCHEDULE_ID)
		)).willReturn(List.of());

		SchedulesResponseDto result = scheduleService.update(updateRequest, PRODUCT_ID, SCHEDULE_ID, SELLER_ID);

		assertThat(result.id()).isEqualTo(SCHEDULE_ID);
		assertThat(result.startTime()).isEqualTo(LocalTime.of(13, 0));
		assertThat(result.endTime()).isEqualTo(LocalTime.of(14, 0));
		assertThat(result.capacity()).isEqualTo(5);
	}

	@Test
	void 일정이_없으면_일정_수정에_실패한다() {
		given(productUseCase.findByIdOrThrow(PRODUCT_ID)).willReturn(product);
		given(scheduleRepository.findByIdAndDeleteDtIsNull(SCHEDULE_ID)).willReturn(Optional.empty());

		assertBusinessException(() -> scheduleService.update(updateRequest, PRODUCT_ID, SCHEDULE_ID, SELLER_ID),
			HttpStatus.NOT_FOUND, CommonErrorCode.SCHDULES_NOT_FOUND);
	}

	@Test
	void 일정_삭제에_성공한다() {
		given(productUseCase.findByIdOrThrow(PRODUCT_ID)).willReturn(product);
		given(productUseCase.matchProductAndSellerId(PRODUCT_ID, SELLER_ID)).willReturn(product);
		given(scheduleRepository.findByIdAndDeleteDtIsNull(SCHEDULE_ID)).willReturn(Optional.of(schedule));

		DeleteScheduleResposeDto result = scheduleService.delete(PRODUCT_ID, SCHEDULE_ID, SELLER_ID);

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
			.capacity(8)
			.build();
		ReflectionTestUtils.setField(secondSchedule, "id", UUID.randomUUID());
		given(scheduleRepository.findByProductIdAndDeleteDtIsNull(PRODUCT_ID)).willReturn(List.of(schedule, secondSchedule));

		List<SchedulesResponseDto> result = scheduleService.schedulesList(PRODUCT_ID);

		assertThat(result).hasSize(2);
		assertThat(result.get(0).capacity()).isEqualTo(10);
		assertThat(result.get(1).capacity()).isEqualTo(8);
	}

	@Test
	void 예약자_ID로_일정_단건을_조회한다() {
		ProductUser user = ProductUser.builder()
			.productScheduleId(SCHEDULE_ID)
			.userId(USER_ID)
			.guestCount(2)
			.status(ReservationStatus.RESERVED)
			.build();
		ReflectionTestUtils.setField(user, "id", PRODUCT_USER_ID);
		given(productUserUseCase.innerFindById(PRODUCT_USER_ID)).willReturn(user);
		given(scheduleRepository.findByIdAndDeleteDtIsNull(SCHEDULE_ID)).willReturn(Optional.of(schedule));

		SchedulesResponseDto result = scheduleService.selectSchedules(PRODUCT_USER_ID);

		assertThat(result.id()).isEqualTo(SCHEDULE_ID);
		assertThat(result.productId()).isEqualTo(PRODUCT_ID);
		assertThat(result.capacity()).isEqualTo(10);
	}

	@Test
	void 가격이_다르면_가격불일치를_반환한다() {
		OrderRequestDto request = new OrderRequestDto(SCHEDULE_ID, USER_ID, 3, new BigDecimal("999.99"));
		given(scheduleRepository.findByIdAndDeleteDtIsNull(SCHEDULE_ID)).willReturn(Optional.of(schedule));
		given(productUseCase.findByIdOrThrow(PRODUCT_ID)).willReturn(product);

		OrderResponseDto result = scheduleService.verification(request);

		assertThat(result.valid()).isEqualTo(OrderValid.PRICE_MISMATCH);
		assertThat(result.productUserId()).isNull();
		then(productUserUseCase).should(never()).create(any(CreateProductUserRequestDto.class));
	}

	@Test
	void 재고가_없으면_품절상태를_반환한다() {
		OrderRequestDto request = new OrderRequestDto(SCHEDULE_ID, USER_ID, 11, PRICE);
		given(scheduleRepository.findByIdAndDeleteDtIsNull(SCHEDULE_ID)).willReturn(Optional.of(schedule));
		given(productUseCase.findByIdOrThrow(PRODUCT_ID)).willReturn(product);
		given(scheduleRepository.verification(11, SCHEDULE_ID)).willReturn(0);

		OrderResponseDto result = scheduleService.verification(request);

		assertThat(result.valid()).isEqualTo(OrderValid.OUT_OF_STOCK);
		assertThat(result.productUserId()).isNull();
		then(productUserUseCase).should(never()).create(any(CreateProductUserRequestDto.class));
	}

	@Test
	void 예약_검증에_성공하면_예약정보를_생성한다() {
		OrderRequestDto request = new OrderRequestDto(SCHEDULE_ID, USER_ID, 3, PRICE);
		ProductUserResponseDto createdUser = new ProductUserResponseDto(
			PRODUCT_USER_ID,
			SCHEDULE_ID,
			"guest-user",
			3,
			ReservationStatus.RESERVED.getStatusName()
		);
		given(scheduleRepository.findByIdAndDeleteDtIsNull(SCHEDULE_ID)).willReturn(Optional.of(schedule));
		given(productUseCase.findByIdOrThrow(PRODUCT_ID)).willReturn(product);
		given(scheduleRepository.verification(3, SCHEDULE_ID)).willReturn(1);
		given(productUserUseCase.create(any(CreateProductUserRequestDto.class))).willReturn(createdUser);

		OrderResponseDto result = scheduleService.verification(request);

		assertThat(result.valid()).isEqualTo(OrderValid.OK);
		assertThat(result.productUserId()).isEqualTo(PRODUCT_USER_ID);

		ArgumentCaptor<CreateProductUserRequestDto> captor = ArgumentCaptor.forClass(CreateProductUserRequestDto.class);
		then(productUserUseCase).should().create(captor.capture());
		assertThat(captor.getValue().productScheduleId()).isEqualTo(SCHEDULE_ID);
		assertThat(captor.getValue().userId()).isEqualTo(USER_ID);
		assertThat(captor.getValue().guestCount()).isEqualTo(3);
		assertThat(captor.getValue().status()).isEqualTo(ReservationStatus.RESERVED);
	}

	@Test
	void 결제완료_상태변경에_성공한다() {
		ProductUser user = ProductUser.builder()
			.productScheduleId(SCHEDULE_ID)
			.userId(USER_ID)
			.guestCount(2)
			.status(ReservationStatus.RESERVED)
			.build();
		ReflectionTestUtils.setField(user, "id", PRODUCT_USER_ID);
		given(productUserUseCase.innerFindById(PRODUCT_USER_ID)).willReturn(user);
		given(scheduleRepository.findByIdAndDeleteDtIsNull(SCHEDULE_ID)).willReturn(Optional.of(schedule));
		given(scheduleRepository.updateStatus(PRODUCT_USER_ID, ReservationStatus.CONFIRMED,
			List.of(ReservationStatus.RESERVED))).willReturn(1);

		OrderValid result = scheduleService.reservationCompleted(PRODUCT_USER_ID);

		assertThat(result).isEqualTo(OrderValid.OK);
	}

	@Test
	void 결제완료_상태변경_대상이_없으면_실패한다() {
		ProductUser user = ProductUser.builder()
			.productScheduleId(SCHEDULE_ID)
			.userId(USER_ID)
			.guestCount(2)
			.status(ReservationStatus.RESERVED)
			.build();
		ReflectionTestUtils.setField(user, "id", PRODUCT_USER_ID);
		given(productUserUseCase.innerFindById(PRODUCT_USER_ID)).willReturn(user);
		given(scheduleRepository.findByIdAndDeleteDtIsNull(SCHEDULE_ID)).willReturn(Optional.of(schedule));
		given(scheduleRepository.updateStatus(PRODUCT_USER_ID, ReservationStatus.CONFIRMED,
			List.of(ReservationStatus.RESERVED))).willReturn(0);

		OrderValid result = scheduleService.reservationCompleted(PRODUCT_USER_ID);

		assertThat(result).isEqualTo(OrderValid.MODI_FAIL);
	}

	@Test
	void 복구_선점에_실패하면_재고복구에_실패한다() {
		given(scheduleRepository.claimRestore(PRODUCT_USER_ID)).willReturn(0);

		OrderValid result = scheduleService.restoringInventory(PRODUCT_USER_ID, ReservationStatus.RELEASED);

		assertThat(result).isEqualTo(OrderValid.MODI_FAIL);
		then(scheduleRepository).should(never()).restoreCapacity(any(UUID.class), any(Integer.class), any(Integer.class));
	}

	@Test
	void 재고_복구와_상태변경에_성공한다() {
		ProductUser user = ProductUser.builder()
			.productScheduleId(SCHEDULE_ID)
			.userId(USER_ID)
			.guestCount(2)
			.status(ReservationStatus.CONFIRMED)
			.build();
		ReflectionTestUtils.setField(user, "id", PRODUCT_USER_ID);
		given(scheduleRepository.claimRestore(PRODUCT_USER_ID)).willReturn(1);
		given(productUserUseCase.innerFindById(PRODUCT_USER_ID)).willReturn(user);
		given(scheduleRepository.findByIdAndDeleteDtIsNull(SCHEDULE_ID)).willReturn(Optional.of(schedule));
		given(productUseCase.findByIdOrThrow(PRODUCT_ID)).willReturn(product);
		given(scheduleRepository.restoreCapacity(SCHEDULE_ID, 2, 10)).willReturn(1);
		given(scheduleRepository.updateStatus(PRODUCT_USER_ID, ReservationStatus.RELEASED,
			List.of(ReservationStatus.RESERVED, ReservationStatus.CONFIRMED))).willReturn(1);

		OrderValid result = scheduleService.restoringInventory(PRODUCT_USER_ID, ReservationStatus.RELEASED);

		assertThat(result).isEqualTo(OrderValid.OK);
		then(scheduleRepository).should(never()).restoreStatus(PRODUCT_USER_ID);
	}

	@Test
	void 재고_복구에_실패하면_복구선점을_원복한다() {
		ProductUser user = ProductUser.builder()
			.productScheduleId(SCHEDULE_ID)
			.userId(USER_ID)
			.guestCount(2)
			.status(ReservationStatus.CONFIRMED)
			.build();
		ReflectionTestUtils.setField(user, "id", PRODUCT_USER_ID);
		given(scheduleRepository.claimRestore(PRODUCT_USER_ID)).willReturn(1);
		given(productUserUseCase.innerFindById(PRODUCT_USER_ID)).willReturn(user);
		given(scheduleRepository.findByIdAndDeleteDtIsNull(SCHEDULE_ID)).willReturn(Optional.of(schedule));
		given(productUseCase.findByIdOrThrow(PRODUCT_ID)).willReturn(product);
		given(scheduleRepository.restoreCapacity(SCHEDULE_ID, 2, 10)).willReturn(0);

		assertBusinessException(
			() -> scheduleService.restoringInventory(PRODUCT_USER_ID, ReservationStatus.RELEASED),
			HttpStatus.CONFLICT,
			CommonErrorCode.RESTORE_INVENTORY_FAILED
		);
	}

	@Test
	void 상태변경에_실패하면_복구선점을_원복한다() {
		ProductUser user = ProductUser.builder()
			.productScheduleId(SCHEDULE_ID)
			.userId(USER_ID)
			.guestCount(2)
			.status(ReservationStatus.CONFIRMED)
			.build();
		ReflectionTestUtils.setField(user, "id", PRODUCT_USER_ID);
		given(scheduleRepository.claimRestore(PRODUCT_USER_ID)).willReturn(1);
		given(productUserUseCase.innerFindById(PRODUCT_USER_ID)).willReturn(user);
		given(scheduleRepository.findByIdAndDeleteDtIsNull(SCHEDULE_ID)).willReturn(Optional.of(schedule));
		given(productUseCase.findByIdOrThrow(PRODUCT_ID)).willReturn(product);
		given(scheduleRepository.restoreCapacity(SCHEDULE_ID, 2, 10)).willReturn(1);
		given(scheduleRepository.updateStatus(PRODUCT_USER_ID, ReservationStatus.REFUNDED,
			List.of(ReservationStatus.RESERVED, ReservationStatus.CONFIRMED))).willReturn(0);

		assertBusinessException(
			() -> scheduleService.restoringInventory(PRODUCT_USER_ID, ReservationStatus.REFUNDED),
			HttpStatus.CONFLICT,
			CommonErrorCode.UPDATE_RESERVATION_STATUS_FAILED
		);
	}

	@Test
	void 일정별_예약_상태를_조회한다() {
		given(scheduleRepository.findByIdAndDeleteDtIsNull(SCHEDULE_ID)).willReturn(Optional.of(schedule));
		given(productUseCase.findByIdOrThrow(PRODUCT_ID)).willReturn(product);

		AvailabilityScheduleResponseDto result = scheduleService.availabilitySchedule(SCHEDULE_ID);

		assertThat(result.scheduleId()).isEqualTo(SCHEDULE_ID);
		assertThat(result.maxCapacity()).isEqualTo(10);
		assertThat(result.reservedCount()).isEqualTo(0);
		assertThat(result.remainingCount()).isEqualTo(10);
	}

	@Test
	void 만료된_일정을_마감처리한다() {
		List<UUID> closableIds = List.of(SCHEDULE_ID);
		given(scheduleRepository.findClosableIds(
			any(LocalDate.class),
			eq(List.of(ReservedStatus.CLOSED, ReservedStatus.FULL)),
			any()
		)).willReturn(closableIds);
		given(scheduleRepository.bulkClose(
			closableIds,
			ReservedStatus.AVAILABLE,
			ReservedStatus.CLOSED
		)).willReturn(1);

		int result = scheduleService.closeExpiredSchedulesOnce();

		assertThat(result).isEqualTo(1);
	}

	@Test
	void 마감할_일정이_없으면_0을_반환한다() {
		given(scheduleRepository.findClosableIds(
			any(LocalDate.class),
			eq(List.of(ReservedStatus.CLOSED, ReservedStatus.FULL)),
			any()
		)).willReturn(List.of());

		int result = scheduleService.closeExpiredSchedulesOnce();

		assertThat(result).isZero();
		then(scheduleRepository).should(never()).bulkClose(any(), any(), any());
	}

	@Test
	void 일정_생성요청_DTO_검증에_실패한다() {
		CreateScheduleRequestDto request = new CreateScheduleRequestDto("", "9:00", "14-00", ReservedStatus.AVAILABLE);

		Set<ConstraintViolation<CreateScheduleRequestDto>> violations = validator.validate(request);

		assertThat(violations).extracting(ConstraintViolation::getPropertyPath)
			.anyMatch(path -> path.toString().equals("scheduleDt"))
			.anyMatch(path -> path.toString().equals("startTime"))
			.anyMatch(path -> path.toString().equals("endTime"));
	}

	@Test
	void 일정_수정요청_DTO_검증에_실패한다() {
		UpdateScheduleRequestDto request = new UpdateScheduleRequestDto("9:00", "14-00", ReservedStatus.AVAILABLE, 0);

		Set<ConstraintViolation<UpdateScheduleRequestDto>> violations = validator.validate(request);

		assertThat(violations).extracting(ConstraintViolation::getPropertyPath)
			.anyMatch(path -> path.toString().equals("startTime"))
			.anyMatch(path -> path.toString().equals("endTime"))
			.anyMatch(path -> path.toString().equals("maxCapacity"));
	}

	private void assertBusinessException(ThrowingCall call, HttpStatus status, CommonErrorCode errorCode) {
		assertThatThrownBy(call::invoke)
			.isInstanceOf(BusinessException.class)
			.hasMessage(errorCode.getMessage())
			.extracting(throwable -> ((BusinessException) throwable).getStatus())
			.isEqualTo(status);
	}

	@FunctionalInterface
	private interface ThrowingCall {
		void invoke();
	}
}
