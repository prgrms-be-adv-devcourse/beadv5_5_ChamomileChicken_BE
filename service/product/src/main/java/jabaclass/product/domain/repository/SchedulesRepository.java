package jabaclass.product.domain.repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.repository.query.Param;

import jabaclass.product.domain.model.Schedules;

public interface SchedulesRepository {

	Schedules save(Schedules schedules);

	List<Schedules> findConflictSchedules(
		@Param("productId") UUID productId,
		@Param("scheduleDt") LocalDate scheduleDt,
		@Param("startTime") LocalTime startTime,
		@Param("endTime") LocalTime endTime
	);

	List<Schedules> findConflictSchedulesNoId(
		@Param("productId") UUID productId,
		@Param("scheduleDt") LocalDate scheduleDt,
		@Param("startTime") LocalTime startTime,
		@Param("endTime") LocalTime endTime,
		@Param("id") UUID id
	);

	Optional<Schedules> findById(UUID schedulesId);

}
