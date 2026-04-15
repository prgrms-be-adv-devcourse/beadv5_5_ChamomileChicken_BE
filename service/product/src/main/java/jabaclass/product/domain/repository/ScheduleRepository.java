package jabaclass.product.domain.repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.query.Param;

import jabaclass.product.domain.model.Schedule;
import jabaclass.product.domain.model.status.ReservationStatus;
import jabaclass.product.domain.model.status.ReservedStatus;

public interface ScheduleRepository {

	Schedule save(Schedule schedule);

	List<Schedule> findConflictSchedules(
		@Param("productId") UUID productId,
		@Param("scheduleDt") LocalDate scheduleDt,
		@Param("startTime") LocalTime startTime,
		@Param("endTime") LocalTime endTime
	);

	List<Schedule> findConflictSchedulesNoId(
		@Param("productId") UUID productId,
		@Param("scheduleDt") LocalDate scheduleDt,
		@Param("startTime") LocalTime startTime,
		@Param("endTime") LocalTime endTime,
		@Param("id") UUID id
	);

	Optional<Schedule> findByIdAndDeleteDtIsNull(UUID schedulesId);

	List<Schedule> findByProductIdAndDeleteDtIsNull(UUID productId);

	// 2026-04-09 추가, 재고 검증 및 재고 차감, 상태값 변경
	int verification(@Param("quantity") int quantity, @Param("scheduleId") UUID scheduleId);

	// 2026-04-09 상태값 변경
	int updateStatus(@Param("productUserId") UUID productUserId,
		@Param("status") ReservationStatus status,
		@Param("conditionStatus") List<ReservationStatus> conditionStatus);

	// 2026-04-09 멱등성 체크 및 선점
	int claimRestore(
		@Param("productUserId") UUID productUserId
	);

	// 2026-04-09 재고 복구
	int restoreCapacity(
		@Param("scheduleId") UUID scheduleId,
		@Param("quantity") int quantity,
		@Param("maxCapacity") int maxCapacity
	);

	// 2026-04-13 재고 복구 과정에서 실패 시 복구 선점 해제
	int restoreStatus(
		@Param("productUserId") UUID productUserId
	);

	/*
	 * 날짜 마감 스케줄러 돌리는 구간
	 *
	 *  */
	List<UUID> findClosableIds(
		@Param("today") LocalDate today,
		@Param("status") ReservedStatus status,
		Pageable pageable
	);

	int bulkClose(
		@Param("ids") List<UUID> ids,
		@Param("status") List<ReservedStatus> status,
		@Param("closedStatus") ReservedStatus closedStatus
	);
}
