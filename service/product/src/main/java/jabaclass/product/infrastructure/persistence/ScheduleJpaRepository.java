package jabaclass.product.infrastructure.persistence;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jabaclass.product.domain.model.Schedule;
import jabaclass.product.domain.model.status.ReservationStatus;
import jabaclass.product.domain.model.status.ReservedStatus;
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
	@Modifying
	@Query("""
			UPDATE Schedule s
		 		SET s.capacity = s.capacity - :quantity,
		     		s.status = CASE
		         WHEN s.capacity - :quantity = 0 THEN 'FULL'
		         ELSE s.status
		     	END
		 		WHERE s.id = :scheduleId
		   	AND s.capacity >= :quantity
		""")
	int verification(@Param("quantity") int quantity, @Param("scheduleId") UUID scheduleId);

	// 2026-04-09 상태값 변경
	@Modifying
	@Query("""
			update ProductUser u set u.status = :status
			where u.id = :productUserId 
					and u.status in :conditionStatus
		""")
	int updateStatus(@Param("productUserId") UUID productUserId,
		@Param("status") ReservationStatus status,
		@Param("conditionStatus") List<ReservationStatus> conditionStatus
	);

	// 2026-04-16 멱등성 체크 및 선점
	@Modifying
	@Query("""
		   UPDATE ProductUser pu
		      SET pu.restoreStatus = 1
		    WHERE pu.id = :productUserId
			  AND pu.status IN ('RESERVED', 'CONFIRMED')
			  AND pu.restoreStatus = 0
		""")
	int claimRestore(
		@Param("productUserId") UUID productUserId
	);

	// 2026-04-09 재고 복구
	@Modifying
	@Query("""
		    UPDATE Schedule s
		       SET s.capacity = s.capacity + :quantity
		     WHERE s.id = :scheduleId
		       AND s.capacity + :quantity <= :maxCapacity
		""")
	int restoreCapacity(
		@Param("scheduleId") UUID scheduleId,
		@Param("quantity") int quantity,
		@Param("maxCapacity") int maxCapacity
	);

	// 2026-04-16 멱등성 체크 및 선점
	@Modifying
	@Query("""
		   UPDATE ProductUser pu
		      SET pu.restoreStatus = 0
		    WHERE pu.id = :productUserId
			  AND pu.restoreStatus = 1
		""")
	int restoreStatus(
		@Param("productUserId") UUID productUserId
	);

	/*
	 * 날짜 마감 스케줄러 돌리는 구간
	 *
	 *  */
	// 사이즈를 가져와 날짜 마감 시킬 아이디 가져오기
	@Query("""
		    select s.id
		    from Schedule s
		    where s.scheduleDt < :today
		      and s.status not in (:status)
		""")
	List<UUID> findClosableIds(
		@Param("today") LocalDate today,
		@Param("status") ReservedStatus status,
		Pageable pageable
	);

	/*
	 * @Modifying(clearAutomatically = true, flushAutomatically = true)
	 * DB랑 JPA 영속성 컨텍스트를 맞추기 위한 안전장치
	 * clearAutomatically = bulk query 실행 후, 영속성 컨텍스트를 싹 비움(같은 트랜잭션 안에서 이미 조회한 엔티티가 있을 때 문제)
	 * 메모리 방지용으로 설정
	 * */
	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query("""
		    update Schedule s
		       set s.status = :closedStatus
		     where s.id in (:ids)
		       and s.status in (:status)
		""")
	int bulkClose(
		@Param("ids") List<UUID> ids,
		@Param("status") List<ReservedStatus> status,
		@Param("closedStatus") ReservedStatus closedStatus
	);

	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query("UPDATE Schedule s SET s.deleteDt = CURRENT_TIMESTAMP WHERE s.productId = :productId AND s.deleteDt IS NULL")
	int softDeleteByProductId(@Param("productId") UUID productId);

	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query("UPDATE Schedule s SET s.deleteDt = NULL WHERE s.productId = :productId AND s.deleteDt IS NOT NULL")
	int restoreDeleteByProductId(@Param("productId") UUID productId);

}
