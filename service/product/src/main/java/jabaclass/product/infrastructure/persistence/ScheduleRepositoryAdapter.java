package jabaclass.product.infrastructure.persistence;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jabaclass.product.domain.model.Schedule;
import jabaclass.product.domain.model.status.ReservationStatus;
import jabaclass.product.domain.model.status.ReservedStatus;
import jabaclass.product.domain.repository.ScheduleRepository;
import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class ScheduleRepositoryAdapter implements ScheduleRepository {

	private final ScheduleJpaRepository scheduleJpaRepository;

	@Override
	public Schedule save(Schedule schedule) {
		return scheduleJpaRepository.save(schedule);
	}

	@Override
	public List<Schedule> findConflictSchedules(
		@Param("productId") UUID productId,
		@Param("scheduleDt") LocalDate scheduleDt,
		@Param("startTime") LocalTime startTime,
		@Param("endTime") LocalTime endTime) {
		return scheduleJpaRepository.findConflictSchedules(productId, scheduleDt, startTime, endTime);
	}

	@Override
	public List<Schedule> findConflictSchedulesNoId(
		@Param("productId") UUID productId,
		@Param("scheduleDt") LocalDate scheduleDt,
		@Param("startTime") LocalTime startTime,
		@Param("endTime") LocalTime endTime,
		@Param("id") UUID id) {
		return scheduleJpaRepository.findConflictSchedulesNoId(productId, scheduleDt, startTime, endTime, id);
	}

	@Override
	public Optional<Schedule> findByIdAndDeleteDtIsNull(UUID schedulesId) {
		return scheduleJpaRepository.findByIdAndDeleteDtIsNull(schedulesId);
	}

	@Override
	public List<Schedule> findByProductIdAndDeleteDtIsNull(UUID productId) {
		return scheduleJpaRepository.findByProductIdAndDeleteDtIsNull(productId);
	}

	@Override
	public int verification(int quantity, UUID scheduleId) {
		return scheduleJpaRepository.verification(quantity, scheduleId);
	}

	@Override
	public int updateStatus(UUID productUserId, ReservationStatus status, List<ReservationStatus> conditionStatus) {
		return scheduleJpaRepository.updateStatus(productUserId, status, conditionStatus);
	}

	@Override
	public int claimRestore(UUID productUserId) {
		return scheduleJpaRepository.claimRestore(productUserId);
	}

	@Override
	public int restoreCapacity(UUID scheduleId, int quantity, int maxCapacity) {
		return scheduleJpaRepository.restoreCapacity(scheduleId, quantity, maxCapacity);
	}

	@Override
	public int restoreStatus(UUID productUserId) {
		return scheduleJpaRepository.restoreStatus(productUserId);
	}

	@Override
	public List<UUID> findClosableIds(LocalDate today, List<ReservedStatus> status, Pageable pageable) {
		return scheduleJpaRepository.findClosableIds(today, status, pageable);
	}

	@Override
	public int bulkClose(List<UUID> ids, ReservedStatus openStatus, ReservedStatus closedStatus) {
		return scheduleJpaRepository.bulkClose(ids, openStatus, closedStatus);
	}

}
