package jabaclass.product.infrastructure.persistence;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jabaclass.product.domain.model.Schedules;
import jabaclass.product.domain.repository.SchedulesRepository;
import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class SchedulesRepositoryAdapter implements SchedulesRepository {

	private final SchedulesJpaRepository schedulesJpaRepository;

	@Override
	public Schedules save(Schedules schedules) {
		return schedulesJpaRepository.save(schedules);
	}

	@Override
	public List<Schedules> findConflictSchedules(
		@Param("productId") UUID productId,
		@Param("scheduleDt") LocalDate scheduleDt,
		@Param("startTime") LocalTime startTime,
		@Param("endTime") LocalTime endTime) {
		return schedulesJpaRepository.findConflictSchedules(productId, scheduleDt, startTime, endTime);
	}

	@Override
	public List<Schedules> findConflictSchedulesNoId(
		@Param("productId") UUID productId,
		@Param("scheduleDt") LocalDate scheduleDt,
		@Param("startTime") LocalTime startTime,
		@Param("endTime") LocalTime endTime,
		@Param("id") UUID id) {
		return schedulesJpaRepository.findConflictSchedulesNoId(productId, scheduleDt, startTime, endTime, id);
	}

	@Override
	public Optional<Schedules> findById(UUID schedulesId) {
		return schedulesJpaRepository.findById(schedulesId);
	}
}
