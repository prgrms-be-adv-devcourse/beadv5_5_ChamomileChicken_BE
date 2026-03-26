package jabaclass.product.domain.model;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.UUID;

import org.hibernate.annotations.UuidGenerator;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jabaclass.product.application.exception.BusinessException;
import jabaclass.product.common.exception.CommonErrorCode;
import jabaclass.product.domain.model.status.ReservedStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import lombok.extern.slf4j.Slf4j;

@Entity
@Getter
@NoArgsConstructor
@SuperBuilder
@EntityListeners(AuditingEntityListener.class)
@Table(name = "products_schedule", schema = "public")
@Slf4j
public class Schedule extends EntityBase {

	@Id
	@UuidGenerator
	@Column(name = "id", updatable = false, nullable = false)
	private UUID id;

	@Column(name = "product_id", nullable = false)
	private UUID productId;

	@Column(name = "schedule_dt", nullable = false)
	private LocalDate scheduleDt;

	@Column(name = "start_time", nullable = false)
	private LocalTime startTime;

	@Column(name = "end_time", nullable = false)
	private LocalTime endTime;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private ReservedStatus status;

	@Column(name = "max_capacity")
	private int maxCapacity;

	@PrePersist
	public void prePersist() {
		if (this.status == null) {
			this.status = ReservedStatus.AVAILABLE;
		}
	}

	public void changeStartTime(LocalTime startTime) {
		if (startTime == null) {
			throw new BusinessException(CommonErrorCode.STTIME_NOT_FOUND);
		}
		this.startTime = startTime;
	}

	public void changeEndTime(LocalTime endTime) {
		if (endTime == null) {
			throw new BusinessException(CommonErrorCode.EDTIME_NOT_FOUND);
		}
		this.endTime = endTime;
	}

	public void changeStatus(ReservedStatus status) {
		this.status = status;
	}

	public void changeMaxCapacity(int maxCapacity) {
		this.maxCapacity = maxCapacity;
	}

	public static LocalDate fDt(String dt) {
		LocalDate scheduleDate;

		try {
			scheduleDate = LocalDate.parse(dt);
		} catch (DateTimeParseException e) {
			throw new BusinessException(CommonErrorCode.DATE_BAD_FORMAT);
		}
		return scheduleDate;
	}

	public static LocalTime fTime(String time) {
		LocalTime ltime;

		try {
			ltime = LocalTime.parse(time);
		} catch (DateTimeParseException e) {
			throw new BusinessException(CommonErrorCode.TIME_BAD_FORMAT);
		}
		return ltime;

	}

}
