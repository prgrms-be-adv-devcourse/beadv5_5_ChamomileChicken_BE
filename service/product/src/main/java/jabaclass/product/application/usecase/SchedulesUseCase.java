package jabaclass.product.application.usecase;

import java.util.UUID;

import jabaclass.product.presentation.dto.request.CreateSchedulesRequestDto;
import jabaclass.product.presentation.dto.request.UpdateSchedulesRequestDto;
import jabaclass.product.presentation.dto.respose.SchedulesResponseDto;

public interface SchedulesUseCase {

	// 스케줄 생성
	SchedulesResponseDto create(CreateSchedulesRequestDto requestDto, UUID productId);

	// 스케줄 삭제
	SchedulesResponseDto delete(CreateSchedulesRequestDto requestDto);

	// 스게줄 수정
	SchedulesResponseDto update(UpdateSchedulesRequestDto requestDto, UUID productId, UUID scheduleId);

	// 스케줄 검색
	SchedulesResponseDto select(CreateSchedulesRequestDto requestDto);

}
