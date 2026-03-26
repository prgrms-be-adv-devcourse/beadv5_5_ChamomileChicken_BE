package jabaclass.product;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.lenient;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Collections;
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
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.fasterxml.jackson.databind.ObjectMapper;

import jabaclass.product.application.acl.SellerRepository;
import jabaclass.product.application.exception.BusinessException;
import jabaclass.product.application.service.AuditorAwareService;
import jabaclass.product.application.service.SchedulesService;
import jabaclass.product.application.usecase.ProductUseCase;
import jabaclass.product.application.usecase.SchedulesUseCase;
import jabaclass.product.common.exception.CommonErrorCode;
import jabaclass.product.domain.model.Products;
import jabaclass.product.domain.model.Schedules;
import jabaclass.product.domain.model.status.ProductStatus;
import jabaclass.product.domain.model.status.ReservedStatus;
import jabaclass.product.domain.repository.SchedulesRepository;
import jabaclass.product.infrastructure.acl.dto.SellerResponseDto;
import jabaclass.product.presentation.ProductRestController;
import jabaclass.product.presentation.dto.request.CreateSchedulesRequestDto;
import jabaclass.product.presentation.dto.request.UpdateSchedulesRequestDto;
import jabaclass.product.presentation.dto.respose.SchedulesResponseDto;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;

@ExtendWith(MockitoExtension.class)
class SchedulesTest {

	@InjectMocks
	private SchedulesService schedulesService;

	@InjectMocks
	private ProductRestController productRestController;

	@Mock
	private SchedulesRepository schedulesRepository;

	@Mock
	private ProductUseCase productUseCase;

	@Mock
	private SellerRepository sellerRepository;

	@Mock
	private ApplicationEventPublisher publisher;

	@Mock
	private AuditorAwareService auditorAwareService;

	@Mock
	private SchedulesUseCase schedulesUseCase;

	private static final UUID SELLER_ID = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
	private static final UUID PRODUCT_ID = UUID.fromString("223e4567-e89b-12d3-a456-426614174000");
	private static final UUID SCHEDULE_ID = UUID.fromString("323e4567-e89b-12d3-a456-426614174000");
	private static final BigDecimal PRICE = new BigDecimal("1000.50");

	private final ObjectMapper objectMapper = new ObjectMapper();

	private Validator validator;
	private MockMvc mockMvc;
	private Products product;
	private Schedules schedule;
	private CreateSchedulesRequestDto 생성요청;
	private UpdateSchedulesRequestDto 수정요청;

	@BeforeEach
	void 설정() {
		validator = Validation.buildDefaultValidatorFactory().getValidator();
		mockMvc = MockMvcBuilders.standaloneSetup(productRestController).build();

		product = Products.builder()
			.id(PRODUCT_ID)
			.sellerId(SELLER_ID)
			.title("product-A")
			.maxCapacity(10)
			.description("test product")
			.descriptionImage(UUID.randomUUID().toString())
			.price(PRICE)
			.status(ProductStatus.ENABLE)
			.build();

		schedule = Schedules.builder()
			.productId(PRODUCT_ID)
			.scheduleDt(LocalDate.now().plusDays(1))
			.startTime(LocalTime.of(10, 0))
			.endTime(LocalTime.of(12, 0))
			.status(ReservedStatus.AVAILABLE)
			.maxCapacity(10)
			.build();
		ReflectionTestUtils.setField(schedule, "id", SCHEDULE_ID);

		생성요청 = new CreateSchedulesRequestDto(
			LocalDate.now().plusDays(1).toString(),
			"10:00",
			"12:00",
			ReservedStatus.AVAILABLE,
			10
		);

		수정요청 = new UpdateSchedulesRequestDto(
			"13:00",
			"14:00",
			ReservedStatus.CLOSED,
			5
		);
	}

	@Test
	void 일정_생성에_성공한다() {
		권한있는_판매자와_본인상품을_준비한다();
		given(schedulesRepository.findConflictSchedules(
			eq(PRODUCT_ID),
			eq(LocalDate.parse(생성요청.scheduleDt())),
			eq(LocalTime.parse(생성요청.startTime())),
			eq(LocalTime.parse(생성요청.endTime()))
		)).willReturn(Collections.emptyList());
		given(schedulesRepository.save(any(Schedules.class)))
			.willAnswer(invocation -> {
				Schedules saved = invocation.getArgument(0);
				ReflectionTestUtils.setField(saved, "id", UUID.randomUUID());
				return saved;
			});

		SchedulesResponseDto result = schedulesService.create(생성요청, PRODUCT_ID);

		assertThat(result.productId()).isEqualTo(PRODUCT_ID);
		assertThat(result.scheduleDt()).isEqualTo(LocalDate.parse(생성요청.scheduleDt()));
		assertThat(result.startTime()).isEqualTo(LocalTime.parse(생성요청.startTime()));
		assertThat(result.endTime()).isEqualTo(LocalTime.parse(생성요청.endTime()));
	}

	@Test
	void 존재하지_않는_날짜면_일정_생성에_실패한다() {
		CreateSchedulesRequestDto request = new CreateSchedulesRequestDto(
			"2026-02-30",
			"10:00",
			"12:00",
			ReservedStatus.AVAILABLE,
			10
		);
		권한있는_판매자와_본인상품을_준비한다();

		비즈니스_예외를_검증한다(
			() -> schedulesService.create(request, PRODUCT_ID),
			HttpStatus.BAD_REQUEST,
			CommonErrorCode.DATE_BAD_FORMAT
		);
	}

	@Test
	void 날짜_형식이_올바르지_않으면_일정_생성에_실패한다() {
		CreateSchedulesRequestDto request = new CreateSchedulesRequestDto(
			"2026/03/26",
			"10:00",
			"12:00",
			ReservedStatus.AVAILABLE,
			10
		);
		권한있는_판매자와_본인상품을_준비한다();

		비즈니스_예외를_검증한다(
			() -> schedulesService.create(request, PRODUCT_ID),
			HttpStatus.BAD_REQUEST,
			CommonErrorCode.DATE_BAD_FORMAT
		);
	}

	@Test
	void 시간_형식이_올바르지_않으면_일정_생성에_실패한다() {
		CreateSchedulesRequestDto request = new CreateSchedulesRequestDto(
			LocalDate.now().plusDays(1).toString(),
			"25:61",
			"12:00",
			ReservedStatus.AVAILABLE,
			10
		);
		권한있는_판매자와_본인상품을_준비한다();

		비즈니스_예외를_검증한다(
			() -> schedulesService.create(request, PRODUCT_ID),
			HttpStatus.BAD_REQUEST,
			CommonErrorCode.TIME_BAD_FORMAT
		);
	}

	@Test
	void 겹치는_일정이_있으면_일정_생성에_실패한다() {
		권한있는_판매자와_본인상품을_준비한다();
		given(schedulesRepository.findConflictSchedules(
			eq(PRODUCT_ID),
			eq(LocalDate.parse(생성요청.scheduleDt())),
			eq(LocalTime.parse(생성요청.startTime())),
			eq(LocalTime.parse(생성요청.endTime()))
		)).willReturn(Collections.singletonList(schedule));

		비즈니스_예외를_검증한다(
			() -> schedulesService.create(생성요청, PRODUCT_ID),
			HttpStatus.BAD_REQUEST,
			CommonErrorCode.SCHEDULE_CONFLICT
		);
	}

	@Test
	void 일정_수정에_성공한다() {
		권한있는_판매자와_본인상품을_준비한다();
		given(schedulesRepository.findById(SCHEDULE_ID)).willReturn(Optional.of(schedule));
		given(schedulesRepository.findConflictSchedulesNoId(
			eq(PRODUCT_ID),
			eq(schedule.getScheduleDt()),
			eq(LocalTime.parse(수정요청.startTime())),
			eq(LocalTime.parse(수정요청.endTime())),
			eq(SCHEDULE_ID)
		)).willReturn(Collections.emptyList());

		SchedulesResponseDto result = schedulesService.update(수정요청, PRODUCT_ID, SCHEDULE_ID);

		assertThat(result.id()).isEqualTo(SCHEDULE_ID);
		assertThat(result.startTime()).isEqualTo(LocalTime.parse("13:00"));
		assertThat(result.endTime()).isEqualTo(LocalTime.parse("14:00"));
		assertThat(result.maxCapacity()).isEqualTo(5);
		assertThat(schedule.getStatus()).isEqualTo(ReservedStatus.CLOSED);
	}

	@Test
	void 수정할_일정이_없으면_일정_수정에_실패한다() {
		권한있는_판매자와_본인상품을_준비한다();
		given(schedulesRepository.findById(SCHEDULE_ID)).willReturn(Optional.empty());

		비즈니스_예외를_검증한다(
			() -> schedulesService.update(수정요청, PRODUCT_ID, SCHEDULE_ID),
			HttpStatus.NOT_FOUND,
			CommonErrorCode.SCHDULES_NOT_FOUND
		);
	}

	@Test
	void 수정_시간_형식이_올바르지_않으면_일정_수정에_실패한다() {
		UpdateSchedulesRequestDto request = new UpdateSchedulesRequestDto(
			"99:99",
			"14:00",
			ReservedStatus.AVAILABLE,
			5
		);
		권한있는_판매자와_본인상품을_준비한다();
		given(schedulesRepository.findById(SCHEDULE_ID)).willReturn(Optional.of(schedule));

		비즈니스_예외를_검증한다(
			() -> schedulesService.update(request, PRODUCT_ID, SCHEDULE_ID),
			HttpStatus.BAD_REQUEST,
			CommonErrorCode.TIME_BAD_FORMAT
		);
	}

	@Test
	void 수정_시작시간이_종료시간과_같거나_늦으면_일정_수정에_실패한다() {
		UpdateSchedulesRequestDto request = new UpdateSchedulesRequestDto(
			"14:00",
			"14:00",
			ReservedStatus.AVAILABLE,
			5
		);
		권한있는_판매자와_본인상품을_준비한다();
		given(schedulesRepository.findById(SCHEDULE_ID)).willReturn(Optional.of(schedule));

		비즈니스_예외를_검증한다(
			() -> schedulesService.update(request, PRODUCT_ID, SCHEDULE_ID),
			HttpStatus.BAD_REQUEST,
			CommonErrorCode.INVALID_TIME_RANGE
		);
	}

	@Test
	void 겹치는_수정_일정이_있으면_일정_수정에_실패한다() {
		권한있는_판매자와_본인상품을_준비한다();
		given(schedulesRepository.findById(SCHEDULE_ID)).willReturn(Optional.of(schedule));
		given(schedulesRepository.findConflictSchedulesNoId(
			eq(PRODUCT_ID),
			eq(schedule.getScheduleDt()),
			eq(LocalTime.parse(수정요청.startTime())),
			eq(LocalTime.parse(수정요청.endTime())),
			eq(SCHEDULE_ID)
		)).willReturn(Collections.singletonList(
			Schedules.builder()
				.productId(PRODUCT_ID)
				.scheduleDt(schedule.getScheduleDt())
				.startTime(LocalTime.of(13, 30))
				.endTime(LocalTime.of(14, 30))
				.status(ReservedStatus.AVAILABLE)
				.maxCapacity(10)
				.build()
		));

		비즈니스_예외를_검증한다(
			() -> schedulesService.update(수정요청, PRODUCT_ID, SCHEDULE_ID),
			HttpStatus.BAD_REQUEST,
			CommonErrorCode.SCHEDULE_CONFLICT
		);
	}

	@Test
	void 판매자_권한이_아니면_일정_수정에_실패한다() {
		given(auditorAwareService.getCurrentAuditor()).willReturn(Optional.of(SELLER_ID));
		given(sellerRepository.findSeller(SELLER_ID))
			.willReturn(Optional.of(new SellerResponseDto(SELLER_ID, "user", "USER")));

		비즈니스_예외를_검증한다(
			() -> schedulesService.update(수정요청, PRODUCT_ID, SCHEDULE_ID),
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

		비즈니스_예외를_검증한다(
			() -> schedulesService.update(수정요청, PRODUCT_ID, SCHEDULE_ID),
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
		given(schedulesUseCase.update(any(UpdateSchedulesRequestDto.class), eq(PRODUCT_ID), eq(SCHEDULE_ID)))
			.willReturn(response);

		mockMvc.perform(put("/api/products/{productId}/schedules/{scheduleId}", PRODUCT_ID, SCHEDULE_ID)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(수정요청)))
			.andExpect(status().isOk())
			.andExpect(content().string(org.hamcrest.Matchers.containsString(PRODUCT_ID.toString())));

		then(schedulesUseCase).should().update(any(UpdateSchedulesRequestDto.class), eq(PRODUCT_ID), eq(SCHEDULE_ID));
	}

	@Test
	void 수정_DTO에서_시작시간_형식이_아니면_검증에_실패한다() {
		UpdateSchedulesRequestDto request = new UpdateSchedulesRequestDto(
			"9:00",
			"14-00",
			ReservedStatus.AVAILABLE,
			0
		);

		Set<ConstraintViolation<UpdateSchedulesRequestDto>> violations = validator.validate(request);

		assertThat(violations).extracting(ConstraintViolation::getPropertyPath)
			.anyMatch(path -> path.toString().equals("startTime"))
			.anyMatch(path -> path.toString().equals("endTime"))
			.anyMatch(path -> path.toString().equals("maxCapacity"));
	}

	private void 권한있는_판매자와_본인상품을_준비한다() {
		given(auditorAwareService.getCurrentAuditor()).willReturn(Optional.of(SELLER_ID));
		given(sellerRepository.findSeller(eq(SELLER_ID)))
			.willReturn(Optional.of(new SellerResponseDto(SELLER_ID, "seller", "SELLER")));
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
