package jabaclass.product.application.usecase;

import java.util.List;
import java.util.UUID;

import jabaclass.product.domain.model.status.ReservationStatus;
import jabaclass.product.presentation.dto.request.CreateScheduleRequestDto;
import jabaclass.product.presentation.dto.request.OrderRequestDto;
import jabaclass.product.presentation.dto.request.UpdateScheduleRequestDto;
import jabaclass.product.presentation.dto.respose.AvailabilityScheduleResponseDto;
import jabaclass.product.presentation.dto.respose.DeleteScheduleResposeDto;
import jabaclass.product.presentation.dto.respose.OrderResponseDto;
import jabaclass.product.presentation.dto.respose.OrderValid;
import jabaclass.product.presentation.dto.respose.SchedulesResponseDto;

public interface ScheduleUseCase {

	// 스케줄 생성
	SchedulesResponseDto create(CreateScheduleRequestDto requestDto, UUID productId, UUID sellerId);

	// 스케줄 삭제
	DeleteScheduleResposeDto delete(UUID productId, UUID scheduleId, UUID sellerId);

	// 스케줄 수정
	SchedulesResponseDto update(UpdateScheduleRequestDto requestDto, UUID productId, UUID scheduleId, UUID sellerId);

	// 스케줄 검색
	List<SchedulesResponseDto> schedulesList(UUID productId);

	// 스케줄 검증 및 재고 차감
	OrderResponseDto verification(OrderRequestDto requestDto);

	// 스케줄 상태 값 변경 2026-04-09 수정
	OrderValid restoringInventory(UUID productUserId, ReservationStatus orderStatus);

	// 스케줄 예약 상태 검색
	AvailabilityScheduleResponseDto availabilitySchedule(UUID scheduleId);

	// 2026-04-09 재고 복구
	int restoreCapacity(UUID scheduleId, int quantity, int capacity);

	// 2026-04-13 결제 성공 시, 예약 확정
	OrderValid reservationCompleted(UUID productUserId);

	// 2026-04-14 -> 예약자 id로 스케줄 정보 가져오기
	SchedulesResponseDto selectSchedules(UUID productUserId);

}
