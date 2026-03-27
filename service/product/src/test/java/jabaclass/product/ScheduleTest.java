package jabaclass.product;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.fasterxml.jackson.databind.ObjectMapper;

import jabaclass.product.application.acl.SellerRepository;
import jabaclass.product.application.exception.BusinessException;
import jabaclass.product.application.service.AuditorAwareService;
import jabaclass.product.application.service.ScheduleService;
import jabaclass.product.application.usecase.ProductUseCase;
import jabaclass.product.application.usecase.ProductUserUseCase;
import jabaclass.product.application.usecase.ScheduleUseCase;
import jabaclass.product.common.exception.CommonErrorCode;
import jabaclass.product.domain.model.Product;
import jabaclass.product.domain.model.ProductUser;
import jabaclass.product.domain.model.Schedule;
import jabaclass.product.domain.model.status.OrderStatus;
import jabaclass.product.domain.model.status.ProductStatus;
import jabaclass.product.domain.model.status.ReservedStatus;
import jabaclass.product.domain.repository.ScheduleRepository;
import jabaclass.product.infrastructure.acl.dto.SellerResponseDto;
import jabaclass.product.presentation.ProductRestController;
import jabaclass.product.presentation.dto.request.CreateProductUserRequestDto;
import jabaclass.product.presentation.dto.request.CreateScheduleRequestDto;
import jabaclass.product.presentation.dto.request.OrderRequestDto;
import jabaclass.product.presentation.dto.request.UpdateScheduleRequestDto;
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

	@InjectMocks
	private ProductRestController productRestController;

	@Mock
	private ScheduleRepository scheduleRepository;

	@Mock
	private ProductUseCase productUseCase;

	@Mock
	private SellerRepository sellerRepository;

	@Mock
	private ApplicationEventPublisher publisher;

	@Mock
	private AuditorAwareService auditorAwareService;

	@Mock
	private ScheduleUseCase scheduleUseCase;

	@Mock
	private ProductUserUseCase productUserUseCase;

	private static final UUID SELLER_ID = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
	private static final UUID PRODUCT_ID = UUID.fromString("223e4567-e89b-12d3-a456-426614174000");
	private static final UUID SCHEDULE_ID = UUID.fromString("323e4567-e89b-12d3-a456-426614174000");
	private static final BigDecimal PRICE = new BigDecimal("1000.50");

	private final ObjectMapper objectMapper = new ObjectMapper();

	private Validator validator;
	private MockMvc mockMvc;
	private Product product;
	private Schedule schedule;
	private CreateScheduleRequestDto createRequest;
	private UpdateScheduleRequestDto updateRequest;

	@BeforeEach
	void setUp() {
		validator = Validation.buildDefaultValidatorFactory().getValidator();
		mockMvc = MockMvcBuilders.standaloneSetup(productRestController).build();

		product = Product.builder()
			.id(PRODUCT_ID)
			.sellerId(SELLER_ID)
			.title("product-A")
			.maxCapacity(10)
			.description("test product")
			.descriptionImage(UUID.randomUUID().toString())
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
		prepareAuthorizedSellerAndOwnedProduct();
		given(scheduleRepository.findConflictSchedules(
			eq(PRODUCT_ID),
			eq(LocalDate.parse(createRequest.scheduleDt())),
			eq(LocalTime.parse(createRequest.startTime())),
			eq(LocalTime.parse(createRequest.endTime()))
		)).willReturn(Collections.emptyList());
		given(scheduleRepository.save(any(Schedule.class)))
			.willAnswer(invocation -> {
				Schedule saved = invocation.getArgument(0);
				ReflectionTestUtils.setField(saved, "id", UUID.randomUUID());
				return saved;
			});

		SchedulesResponseDto result = scheduleService.create(createRequest, PRODUCT_ID);

		assertThat(result.productId()).isEqualTo(PRODUCT_ID);
		assertThat(result.scheduleDt()).isEqualTo(LocalDate.parse(createRequest.scheduleDt()));
		assertThat(result.startTime()).isEqualTo(LocalTime.parse(createRequest.startTime()));
		assertThat(result.endTime()).isEqualTo(LocalTime.parse(createRequest.endTime()));
		assertThat(result.maxCapacity()).isEqualTo(product.getMaxCapacity());
	}

	@Test
	void 존재하지_않는_날짜면_일정_생성에_실패한다() {
		CreateScheduleRequestDto request = new CreateScheduleRequestDto(
			"2026-02-30",
			"10:00",
			"12:00",
			ReservedStatus.AVAILABLE
		);
		prepareAuthorizedSellerAndOwnedProduct();

		assertBusinessException(
			() -> scheduleService.create(request, PRODUCT_ID),
			HttpStatus.BAD_REQUEST,
			CommonErrorCode.DATE_BAD_FORMAT
		);
	}

	@Test
	void 날짜_형식이_올바르지_않으면_일정_생성에_실패한다() {
		CreateScheduleRequestDto request = new CreateScheduleRequestDto(
			"2026/03/26",
			"10:00",
			"12:00",
			ReservedStatus.AVAILABLE
		);
		prepareAuthorizedSellerAndOwnedProduct();

		assertBusinessException(
			() -> scheduleService.create(request, PRODUCT_ID),
			HttpStatus.BAD_REQUEST,
			CommonErrorCode.DATE_BAD_FORMAT
		);
	}

	@Test
	void 시간_형식이_올바르지_않으면_일정_생성에_실패한다() {
		CreateScheduleRequestDto request = new CreateScheduleRequestDto(
			LocalDate.now().plusDays(1).toString(),
			"25:61",
			"12:00",
			ReservedStatus.AVAILABLE
		);
		prepareAuthorizedSellerAndOwnedProduct();

		assertBusinessException(
			() -> scheduleService.create(request, PRODUCT_ID),
			HttpStatus.BAD_REQUEST,
			CommonErrorCode.TIME_BAD_FORMAT
		);
	}

	@Test
	void 겹치는_일정이_있으면_일정_생성에_실패한다() {
		prepareAuthorizedSellerAndOwnedProduct();
		given(scheduleRepository.findConflictSchedules(
			eq(PRODUCT_ID),
			eq(LocalDate.parse(createRequest.scheduleDt())),
			eq(LocalTime.parse(createRequest.startTime())),
			eq(LocalTime.parse(createRequest.endTime()))
		)).willReturn(Collections.singletonList(schedule));

		assertBusinessException(
			() -> scheduleService.create(createRequest, PRODUCT_ID),
			HttpStatus.BAD_REQUEST,
			CommonErrorCode.SCHEDULE_CONFLICT
		);
	}

	@Test
	void 일정_수정에_성공한다() {
		prepareAuthorizedSellerAndOwnedProduct();
		given(scheduleRepository.findById(SCHEDULE_ID)).willReturn(Optional.of(schedule));
		given(scheduleRepository.findConflictSchedulesNoId(
			eq(PRODUCT_ID),
			eq(schedule.getScheduleDt()),
			eq(LocalTime.parse(updateRequest.startTime())),
			eq(LocalTime.parse(updateRequest.endTime())),
			eq(SCHEDULE_ID)
		)).willReturn(Collections.emptyList());

		SchedulesResponseDto result = scheduleService.update(updateRequest, PRODUCT_ID, SCHEDULE_ID);

		assertThat(result.id()).isEqualTo(SCHEDULE_ID);
		assertThat(result.startTime()).isEqualTo(LocalTime.parse("13:00"));
		assertThat(result.endTime()).isEqualTo(LocalTime.parse("14:00"));
		assertThat(result.maxCapacity()).isEqualTo(5);
		assertThat(schedule.getStatus()).isEqualTo(ReservedStatus.CLOSED);
	}

	@Test
	void 수정할_일정이_없으면_일정_수정에_실패한다() {
		prepareAuthorizedSellerAndOwnedProduct();
		given(scheduleRepository.findById(SCHEDULE_ID)).willReturn(Optional.empty());

		assertBusinessException(
			() -> scheduleService.update(updateRequest, PRODUCT_ID, SCHEDULE_ID),
			HttpStatus.NOT_FOUND,
			CommonErrorCode.SCHDULES_NOT_FOUND
		);
	}

	@Test
	void 수정_시간_형식이_올바르지_않으면_일정_수정에_실패한다() {
		UpdateScheduleRequestDto request = new UpdateScheduleRequestDto(
			"99:99",
			"14:00",
			ReservedStatus.AVAILABLE,
			5
		);
		prepareAuthorizedSellerAndOwnedProduct();
		given(scheduleRepository.findById(SCHEDULE_ID)).willReturn(Optional.of(schedule));

		assertBusinessException(
			() -> scheduleService.update(request, PRODUCT_ID, SCHEDULE_ID),
			HttpStatus.BAD_REQUEST,
			CommonErrorCode.TIME_BAD_FORMAT
		);
	}

	@Test
	void 수정_시작시간이_종료시간과_같거나_늦으면_일정_수정에_실패한다() {
		UpdateScheduleRequestDto request = new UpdateScheduleRequestDto(
			"14:00",
			"14:00",
			ReservedStatus.AVAILABLE,
			5
		);
		prepareAuthorizedSellerAndOwnedProduct();
		given(scheduleRepository.findById(SCHEDULE_ID)).willReturn(Optional.of(schedule));

		assertBusinessException(
			() -> scheduleService.update(request, PRODUCT_ID, SCHEDULE_ID),
			HttpStatus.BAD_REQUEST,
			CommonErrorCode.INVALID_TIME_RANGE
		);
	}

	@Test
	void 겹치는_수정_일정이_있으면_일정_수정에_실패한다() {
		prepareAuthorizedSellerAndOwnedProduct();
		given(scheduleRepository.findById(SCHEDULE_ID)).willReturn(Optional.of(schedule));
		given(scheduleRepository.findConflictSchedulesNoId(
			eq(PRODUCT_ID),
			eq(schedule.getScheduleDt()),
			eq(LocalTime.parse(updateRequest.startTime())),
			eq(LocalTime.parse(updateRequest.endTime())),
			eq(SCHEDULE_ID)
		)).willReturn(Collections.singletonList(
			Schedule.builder()
				.productId(PRODUCT_ID)
				.scheduleDt(schedule.getScheduleDt())
				.startTime(LocalTime.of(13, 30))
				.endTime(LocalTime.of(14, 30))
				.status(ReservedStatus.AVAILABLE)
				.maxCapacity(10)
				.build()
		));

		assertBusinessException(
			() -> scheduleService.update(updateRequest, PRODUCT_ID, SCHEDULE_ID),
			HttpStatus.BAD_REQUEST,
			CommonErrorCode.SCHEDULE_CONFLICT
		);
	}

	@Test
	void 판매자_권한이_아니면_일정_수정에_실패한다() {
		given(auditorAwareService.getCurrentAuditor()).willReturn(Optional.of(SELLER_ID));
		given(sellerRepository.findSeller(SELLER_ID))
			.willReturn(Optional.of(new SellerResponseDto(SELLER_ID, "user", "USER")));

		assertBusinessException(
			() -> scheduleService.update(updateRequest, PRODUCT_ID, SCHEDULE_ID),
			HttpStatus.FORBIDDEN,
			CommonErrorCode.NOT_SELLER
		);
	}

	@Test
	void 상품이_존재하지_않으면_일정_수정에_실패한다() {
		given(auditorAwareService.getCurrentAuditor()).willReturn(Optional.of(SELLER_ID));
		given(sellerRepository.findSeller(SELLER_ID))
			.willReturn(Optional.of(new SellerResponseDto(SELLER_ID, "seller", "SELLER")));
		given(productUseCase.findByIdOrThrow(PRODUCT_ID))
			.willThrow(new BusinessException(CommonErrorCode.PRODUCT_NOT_FOUND));

		assertBusinessException(
			() -> scheduleService.update(updateRequest, PRODUCT_ID, SCHEDULE_ID),
			HttpStatus.NOT_FOUND,
			CommonErrorCode.PRODUCT_NOT_FOUND
		);
	}

	@Test
	void 수정_요청이_들어오면_컨트롤러가_유스케이스를_호출한다() throws Exception {
		SchedulesResponseDto response = new SchedulesResponseDto(
			SCHEDULE_ID,
			PRODUCT_ID,
			LocalDate.now().plusDays(1),
			LocalTime.of(13, 0),
			LocalTime.of(14, 0),
			"CLOSED",
			5,
			SELLER_ID,
			LocalDateTime.now(),
			SELLER_ID,
			LocalDateTime.now()
		);
		given(scheduleUseCase.update(any(UpdateScheduleRequestDto.class), eq(PRODUCT_ID), eq(SCHEDULE_ID)))
			.willReturn(response);

		mockMvc.perform(put("/api/v1/products/{productId}/schedules/{scheduleId}", PRODUCT_ID, SCHEDULE_ID)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(updateRequest)))
			.andExpect(status().isOk())
			.andExpect(content().string(org.hamcrest.Matchers.containsString(PRODUCT_ID.toString())));

		then(scheduleUseCase).should().update(any(UpdateScheduleRequestDto.class), eq(PRODUCT_ID), eq(SCHEDULE_ID));
	}

	@Test
	void 수정_DTO에서_시작시간_형식이_아니면_검증에_실패한다() {
		UpdateScheduleRequestDto request = new UpdateScheduleRequestDto(
			"9:00",
			"14-00",
			ReservedStatus.AVAILABLE,
			0
		);

		Set<ConstraintViolation<UpdateScheduleRequestDto>> violations = validator.validate(request);

		assertThat(violations).extracting(ConstraintViolation::getPropertyPath)
			.anyMatch(path -> path.toString().equals("startTime"))
			.anyMatch(path -> path.toString().equals("endTime"))
			.anyMatch(path -> path.toString().equals("maxCapacity"));
	}

	@Test
	void 예약_검증에_성공하면_재고를_차감하고_true를_반환한다() {
		UUID userId = UUID.randomUUID();
		UUID productUserId = UUID.randomUUID();
		OrderRequestDto request = new OrderRequestDto(SCHEDULE_ID, userId, OrderStatus.PENDING, 3, null);
		given(scheduleRepository.findById(SCHEDULE_ID)).willReturn(Optional.of(schedule));
		given(productUserUseCase.innserUserList(SCHEDULE_ID)).willReturn(List.of(
			ProductUser.builder()
				.productScheduleId(SCHEDULE_ID)
				.userId(UUID.randomUUID())
				.guestCount(2)
				.status(OrderStatus.PAID)
				.build()
		));
		given(productUserUseCase.create(any(CreateProductUserRequestDto.class)))
			.willReturn(new ProductUserResponseDto(
				productUserId,
				SCHEDULE_ID,
				"user",
				3,
				OrderStatus.PENDING.getStatusName(),
				userId,
				null
			));
		given(productUseCase.findByIdOrThrow(PRODUCT_ID)).willReturn(product);

		OrderResponseDto result = scheduleService.verification(request);

		assertThat(result.quantity()).isEqualTo(3);
		assertThat(result.valid()).isTrue();
		assertThat(result.price()).isEqualByComparingTo(PRICE);
		assertThat(result.productUserId()).isEqualTo(productUserId);
		then(productUserUseCase).should().create(any(CreateProductUserRequestDto.class));
	}

	@Test
	void 예약_수량이_재고보다_많으면_false를_반환하고_재고를_유지한다() {
		OrderRequestDto request = new OrderRequestDto(
			SCHEDULE_ID,
			UUID.randomUUID(),
			OrderStatus.PENDING,
			11,
			null
		);
		given(scheduleRepository.findById(SCHEDULE_ID)).willReturn(Optional.of(schedule));
		given(productUserUseCase.innserUserList(SCHEDULE_ID)).willReturn(List.of(
			ProductUser.builder()
				.productScheduleId(SCHEDULE_ID)
				.userId(UUID.randomUUID())
				.guestCount(3)
				.status(OrderStatus.PAID)
				.build()
		));
		given(productUseCase.findByIdOrThrow(PRODUCT_ID)).willReturn(product);

		OrderResponseDto result = scheduleService.verification(request);

		assertThat(result.quantity()).isEqualTo(11);
		assertThat(result.valid()).isFalse();
		assertThat(result.price()).isEqualByComparingTo(PRICE);
		assertThat(result.productUserId()).isNull();
		then(productUserUseCase).should().innserUserList(SCHEDULE_ID);
		then(productUserUseCase).should(never()).create(any(CreateProductUserRequestDto.class));
	}

	@Test
	void 재고_복원_요청이_오면_수량만큼_재고를_복구한다() {
		UUID productUserId = UUID.randomUUID();
		ProductUser productUser = ProductUser.builder()
			.productScheduleId(SCHEDULE_ID)
			.userId(UUID.randomUUID())
			.guestCount(2)
			.status(OrderStatus.PAID)
			.build();
		ReflectionTestUtils.setField(productUser, "id", productUserId);
		OrderRequestDto request = new OrderRequestDto(
			SCHEDULE_ID,
			productUser.getUserId(),
			OrderStatus.REFUNDED,
			2,
			productUserId
		);
		given(productUserUseCase.innerFindById(productUserId)).willReturn(productUser);

		scheduleService.restoringInventory(request);

		assertThat(productUser.getStatus()).isEqualTo(OrderStatus.REFUNDED);
		then(productUserUseCase).should().innerFindById(productUserId);
	}

	@Test
	void 예약_검증_요청이_들어오면_컨트롤러가_유스케이스를_호출한다() {
		OrderRequestDto request = new OrderRequestDto(
			SCHEDULE_ID,
			UUID.randomUUID(),
			OrderStatus.PENDING,
			2,
			null
		);
		OrderResponseDto response = new OrderResponseDto(PRICE, 2, true, UUID.randomUUID());
		given(scheduleUseCase.verification(request)).willReturn(response);

		ResponseEntity<OrderResponseDto> result = productRestController.schedulesReservations(request);

		assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(result.getBody()).isEqualTo(response);
		then(scheduleUseCase).should().verification(request);
	}

	@Test
	void 재고_복원_요청이_들어오면_컨트롤러가_유스케이스를_호출한다() {
		OrderRequestDto request = new OrderRequestDto(
			SCHEDULE_ID,
			UUID.randomUUID(),
			OrderStatus.REFUNDED,
			2,
			UUID.randomUUID()
		);

		productRestController.schedulesVerification(request);

		then(scheduleUseCase).should().restoringInventory(request);
	}

	@Test
	void 일정_삭제에_성공한다() {
		prepareAuthorizedSellerAndOwnedProduct();
		given(scheduleRepository.findById(SCHEDULE_ID)).willReturn(Optional.of(schedule));

		DeleteScheduleResposeDto result = scheduleService.delete(PRODUCT_ID, SCHEDULE_ID);

		assertThat(result.scheduleId()).isEqualTo(SCHEDULE_ID);
		assertThat(result.status()).isEqualTo(ReservedStatus.CLOSED);
		assertThat(schedule.getStatus()).isEqualTo(ReservedStatus.CLOSED);
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

		given(scheduleRepository.findByProductIdAndDeleteDtIsNull(PRODUCT_ID))
			.willReturn(List.of(schedule, secondSchedule));

		List<SchedulesResponseDto> result = scheduleService.schedulesList(PRODUCT_ID);

		assertThat(result).hasSize(2);
		assertThat(result).extracting(SchedulesResponseDto::productId)
			.containsOnly(PRODUCT_ID);
	}

	@Test
	void 삭제_요청이_들어오면_컨트롤러가_유스케이스를_호출한다() {
		DeleteScheduleResposeDto response = DeleteScheduleResposeDto.from(SCHEDULE_ID, ReservedStatus.CLOSED);
		given(scheduleUseCase.delete(PRODUCT_ID, SCHEDULE_ID)).willReturn(response);

		ResponseEntity<?> result = productRestController.schedulesDelete(PRODUCT_ID, SCHEDULE_ID);

		assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
		then(scheduleUseCase).should().delete(PRODUCT_ID, SCHEDULE_ID);
	}

	@Test
	void 목록_조회_요청이_들어오면_컨트롤러가_유스케이스를_호출한다() {
		List<SchedulesResponseDto> response = List.of(
			new SchedulesResponseDto(
				SCHEDULE_ID,
				PRODUCT_ID,
				LocalDate.now().plusDays(1),
				LocalTime.of(10, 0),
				LocalTime.of(12, 0),
				"AVAILABLE",
				10,
				SELLER_ID,
				LocalDateTime.now(),
				SELLER_ID,
				LocalDateTime.now()
			)
		);
		given(scheduleUseCase.schedulesList(PRODUCT_ID)).willReturn(response);

		ResponseEntity<?> result = productRestController.schedulesSelectList(PRODUCT_ID);

		assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
		then(scheduleUseCase).should().schedulesList(PRODUCT_ID);
	}

	private void prepareAuthorizedSellerAndOwnedProduct() {
		given(auditorAwareService.getCurrentAuditor()).willReturn(Optional.of(SELLER_ID));
		given(sellerRepository.findSeller(eq(SELLER_ID)))
			.willReturn(Optional.of(new SellerResponseDto(SELLER_ID, "seller", "SELLER")));
		given(productUseCase.findByIdOrThrow(eq(PRODUCT_ID))).willReturn(product);
		lenient().when(productUseCase.matchProductAndSellerId(eq(PRODUCT_ID), eq(SELLER_ID))).thenReturn(product);
	}

	private void assertBusinessException(ThrowingCall call, HttpStatus status, CommonErrorCode errorCode) {
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
