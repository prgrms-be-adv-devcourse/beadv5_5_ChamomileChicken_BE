package jabaclass.product.application.usecase;

import java.util.List;
import java.util.UUID;

import jabaclass.product.presentation.dto.request.CreateScheduleRequestDto;
import jabaclass.product.presentation.dto.request.OrderRequestDto;
import jabaclass.product.presentation.dto.request.UpdateScheduleRequestDto;
import jabaclass.product.presentation.dto.respose.DeleteScheduleResposeDto;
import jabaclass.product.presentation.dto.respose.OrderResponseDto;
import jabaclass.product.presentation.dto.respose.SchedulesResponseDto;

public interface ScheduleUseCase {

	// 스케줄 생성
	SchedulesResponseDto create(CreateScheduleRequestDto requestDto, UUID productId);

	// 스케줄 삭제
	DeleteScheduleResposeDto delete(UUID productId, UUID scheduleId);

	// 스게줄 수정
	SchedulesResponseDto update(UpdateScheduleRequestDto requestDto, UUID productId, UUID scheduleId);

	// 스케줄 검색
	List<SchedulesResponseDto> schedulesList(UUID productId);

	OrderResponseDto verification(OrderRequestDto requestDto);

	void restoringInventory(OrderRequestDto requestDto);

}
