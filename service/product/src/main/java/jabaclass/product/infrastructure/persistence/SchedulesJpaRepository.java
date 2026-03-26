package jabaclass.product.infrastructure.persistence;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jabaclass.product.domain.model.Schedules;
import jakarta.persistence.LockModeType;

public interface SchedulesJpaRepository extends JpaRepository<Schedules, UUID> {

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("""
		    SELECT s 
		    FROM Schedules s
		    WHERE s.productId = :productId
		      AND s.scheduleDt = :scheduleDt
		      AND s.startTime < :endTime
		      AND s.endTime > :startTime
		""")
	List<Schedules> findConflictSchedules(
		@Param("productId") UUID productId,
		@Param("scheduleDt") LocalDate scheduleDt,
		@Param("startTime") LocalTime startTime,
		@Param("endTime") LocalTime endTime
	);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("""
		    SELECT s 
		    FROM Schedules s
		    WHERE s.productId = :productId
		      AND s.scheduleDt = :scheduleDt
		      AND s.startTime < :endTime
		      AND s.endTime > :startTime
			  AND s.id != :id
		""")
	List<Schedules> findConflictSchedulesNoId(
		@Param("productId") UUID productId,
		@Param("scheduleDt") LocalDate scheduleDt,
		@Param("startTime") LocalTime startTime,
		@Param("endTime") LocalTime endTime,
		@Param("id") UUID id
	);
}
