package jabaclass.product;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import jabaclass.product.application.scheduled.ScheduleStatusScheduler;
import jabaclass.product.application.usecase.ScheduleUseCase;

@ExtendWith(MockitoExtension.class)
class ScheduleStatusSchedulerTest {

	@InjectMocks
	private ScheduleStatusScheduler scheduleStatusScheduler;

	@Mock
	private ScheduleUseCase scheduleUseCase;

	@Test
	void 만료_일정_배치를_한번만_돌고_종료한다() {
		given(scheduleUseCase.closeExpiredSchedulesOnce()).willReturn(20);

		scheduleStatusScheduler.closeExpiredSchedules();

		then(scheduleUseCase).should().closeExpiredSchedulesOnce();
	}

	@Test
	void 배치_크기만큼_처리되면_다시_조회한다() {
		given(scheduleUseCase.closeExpiredSchedulesOnce()).willReturn(100, 20);

		scheduleStatusScheduler.closeExpiredSchedules();

		then(scheduleUseCase).should(times(2)).closeExpiredSchedulesOnce();
	}
}
