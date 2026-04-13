package jabaclass.product.infrastructure.persistence;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jabaclass.product.domain.model.Schedule;
import jabaclass.product.domain.model.status.ReservationStatus;
import jakarta.persistence.LockModeType;

public interface ScheduleJpaRepository extends JpaRepository<Schedule, UUID> {

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("""
		    SELECT s 
		    FROM Schedule s
		    WHERE s.productId = :productId
		      AND s.scheduleDt = :scheduleDt
		      AND s.startTime < :endTime
		      AND s.endTime > :startTime
		""")
	List<Schedule> findConflictSchedules(
		@Param("productId") UUID productId,
		@Param("scheduleDt") LocalDate scheduleDt,
		@Param("startTime") LocalTime startTime,
		@Param("endTime") LocalTime endTime
	);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("""
		    SELECT s 
		    FROM Schedule s
		    WHERE s.productId = :productId
		      AND s.scheduleDt = :scheduleDt
		      AND s.startTime < :endTime
		      AND s.endTime > :startTime
			  AND s.id != :id
		""")
	List<Schedule> findConflictSchedulesNoId(
		@Param("productId") UUID productId,
		@Param("scheduleDt") LocalDate scheduleDt,
		@Param("startTime") LocalTime startTime,
		@Param("endTime") LocalTime endTime,
		@Param("id") UUID id
	);

	List<Schedule> findByProductIdAndDeleteDtIsNull(UUID productId);

	Optional<Schedule> findByIdAndDeleteDtIsNull(UUID schedulesId);

	// 2026-04-09 추가, 재고 검증 및 재고 차감, 상태값 변경
	@Query("""
			UPDATE Schedule s
		 		SET s.maxCapacity = s.maxCapacity - :quantity,
		     		s.status = CASE
		         WHEN s.maxCapacity - :quantity = 0 THEN 'FULL'
		         ELSE s.status
		     	END
		 		WHERE s.id = :scheduleId
		   	AND s.maxCapacity >= :quantity
		""")
	int verification(@Param("quantity") int quantity, @Param("scheduleId") UUID scheduleId);

	// 2026-04-09 상태값 변경
	@Modifying
	@Query("update ProductUser u set u.status = :status where u.id = :productUserId")
	int updateStatus(@Param("productUserId") UUID productUserId,
		@Param("status") ReservationStatus status);

	// 2026-04-09 멱등성 체크 및 선점
	@Modifying
	@Query("""
		   UPDATE ProductUser pu
		      SET pu.status = :restoringStatus
		    WHERE pu.id = :productUserId
			  AND pu.status IN ('PENDING', 'PAID')
		""")
	int claimRestore(
		@Param("productUserId") UUID productUserId,
		@Param("restoringStatus") ReservationStatus restoringStatus
	);

	// 2026-04-09 재고 복구
	@Modifying
	@Query("""
		    UPDATE Schedule s
		       SET s.maxCapacity = s.maxCapacity + :quantity
		     WHERE s.id = :scheduleId
		       AND s.maxCapacity + :quantity <= :capacity
		""")
	int restoreCapacity(
		@Param("scheduleId") UUID scheduleId,
		@Param("quantity") int quantity,
		@Param("capacity") int capacity
	);

	// 2026-04-09 예약자 테이블 id로 스케쥴 검색
	@Query("""
		    SELECT s 
			FROM ProductUser s
		     WHERE s.id = :productUserId
		""")
	Optional<Schedule> findByProductUserId(@Param("productUserId") UUID productUserId);

}
