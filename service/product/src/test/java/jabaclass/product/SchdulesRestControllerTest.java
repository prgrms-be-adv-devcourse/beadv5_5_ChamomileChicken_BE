package jabaclass.product;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import jabaclass.product.application.usecase.ScheduleUseCase;
import jabaclass.product.common.exception.ApiResponseDto;
import jabaclass.product.domain.model.status.ReservedStatus;
import jabaclass.product.presentation.dto.respose.OrderValid;
import jabaclass.product.presentation.controller.SchdulesRestController;
import jabaclass.product.presentation.dto.request.CreateScheduleRequestDto;
import jabaclass.product.presentation.dto.request.OrderRequestDto;
import jabaclass.product.presentation.dto.request.UpdateScheduleRequestDto;
import jabaclass.product.presentation.dto.respose.AvailabilityScheduleResponseDto;
import jabaclass.product.presentation.dto.respose.DeleteScheduleResposeDto;
import jabaclass.product.presentation.dto.respose.OrderResponseDto;
import jabaclass.product.presentation.dto.respose.SchedulesResponseDto;

@ExtendWith(MockitoExtension.class)
class SchdulesRestControllerTest {

	@InjectMocks
	private SchdulesRestController schdulesRestController;

	@Mock
	private ScheduleUseCase scheduleUseCase;

	private static final UUID PRODUCT_ID = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
	private static final UUID SCHEDULE_ID = UUID.fromString("223e4567-e89b-12d3-a456-426614174000");
	private static final UUID USER_ID = UUID.fromString("323e4567-e89b-12d3-a456-426614174000");
	private static final UUID PRODUCT_USER_ID = UUID.fromString("423e4567-e89b-12d3-a456-426614174000");

	private CreateScheduleRequestDto createRequest;
	private UpdateScheduleRequestDto updateRequest;
	private SchedulesResponseDto scheduleResponse;

	@BeforeEach
	void setUp() {
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

		scheduleResponse = new SchedulesResponseDto(
			SCHEDULE_ID,
			PRODUCT_ID,
			LocalDate.now().plusDays(1),
			LocalTime.of(10, 0),
			LocalTime.of(12, 0),
			"AVAILABLE",
			10,
			USER_ID,
			LocalDateTime.now(),
			USER_ID,
			LocalDateTime.now()
		);
	}

	@Test
	void 일정_생성_요청이_들어오면_유스케이스를_호출한다() {
		given(scheduleUseCase.create(createRequest, PRODUCT_ID)).willReturn(scheduleResponse);

		ResponseEntity<ApiResponseDto<SchedulesResponseDto>> result = schdulesRestController.schedulesCreate(
			createRequest,
			PRODUCT_ID
		);

		assertThat(result.getStatusCode()).isEqualTo(HttpStatus.CREATED);
		assertThat(result.getBody()).isNotNull();
		assertThat(result.getBody().getData()).isEqualTo(scheduleResponse);
		then(scheduleUseCase).should().create(createRequest, PRODUCT_ID);
	}

	@Test
	void 일정_수정_요청이_들어오면_유스케이스를_호출한다() {
		given(scheduleUseCase.update(updateRequest, PRODUCT_ID, SCHEDULE_ID)).willReturn(scheduleResponse);

		ResponseEntity<ApiResponseDto<SchedulesResponseDto>> result = schdulesRestController.schedulesUpdate(
			updateRequest,
			PRODUCT_ID,
			SCHEDULE_ID
		);

		assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(result.getBody()).isNotNull();
		assertThat(result.getBody().getData()).isEqualTo(scheduleResponse);
		then(scheduleUseCase).should().update(updateRequest, PRODUCT_ID, SCHEDULE_ID);
	}

	@Test
	void 예약_검증_요청이_들어오면_유스케이스를_호출한다() {
		OrderRequestDto request = new OrderRequestDto(SCHEDULE_ID, USER_ID, 2, new BigDecimal("10000"));
		OrderResponseDto response = new OrderResponseDto(new BigDecimal("10000"), 2, OrderValid.OK, PRODUCT_USER_ID);
		given(scheduleUseCase.verification(request)).willReturn(response);

		ResponseEntity<OrderResponseDto> result = schdulesRestController.schedulesReservations(request);

		assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(result.getBody()).isEqualTo(response);
		then(scheduleUseCase).should().verification(request);
	}

	@Test
	void 일정_삭제_요청이_들어오면_유스케이스를_호출한다() {
		DeleteScheduleResposeDto response = DeleteScheduleResposeDto.from(SCHEDULE_ID, ReservedStatus.CLOSED);
		given(scheduleUseCase.delete(PRODUCT_ID, SCHEDULE_ID)).willReturn(response);

		ResponseEntity<ApiResponseDto<DeleteScheduleResposeDto>> result = schdulesRestController.schedulesDelete(
			PRODUCT_ID,
			SCHEDULE_ID
		);

		assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(result.getBody()).isNotNull();
		assertThat(result.getBody().getData()).isEqualTo(response);
		then(scheduleUseCase).should().delete(PRODUCT_ID, SCHEDULE_ID);
	}

	@Test
	void 일정_목록_조회_요청이_들어오면_유스케이스를_호출한다() {
		List<SchedulesResponseDto> response = List.of(scheduleResponse);
		given(scheduleUseCase.schedulesList(PRODUCT_ID)).willReturn(response);

		ResponseEntity<ApiResponseDto<List<SchedulesResponseDto>>> result = schdulesRestController.schedulesSelectList(
			PRODUCT_ID);

		assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(result.getBody()).isNotNull();
		assertThat(result.getBody().getData()).hasSize(1);
		then(scheduleUseCase).should().schedulesList(PRODUCT_ID);
	}

	@Test
	void 일정_예약_상태_조회_요청이_들어오면_유스케이스를_호출한다() {
		AvailabilityScheduleResponseDto response = new AvailabilityScheduleResponseDto(
			SCHEDULE_ID,
			ReservedStatus.AVAILABLE,
			10,
			4,
			6
		);
		given(scheduleUseCase.availabilitySchedule(SCHEDULE_ID)).willReturn(response);

		ResponseEntity<ApiResponseDto<AvailabilityScheduleResponseDto>> result = schdulesRestController
			.schedulesaAvailability(SCHEDULE_ID);

		assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(result.getBody()).isNotNull();
		assertThat(result.getBody().getData()).isEqualTo(response);
		then(scheduleUseCase).should().availabilitySchedule(SCHEDULE_ID);
	}
}
