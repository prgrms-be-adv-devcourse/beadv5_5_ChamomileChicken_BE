package jabaclass.product.domain.model;

import java.util.UUID;

import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jabaclass.product.domain.model.status.ReservationStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Entity
@Getter
@NoArgsConstructor
@SuperBuilder
@EntityListeners(AuditingEntityListener.class)
@Table(name = "products_users", schema = "public")
public class ProductUser extends EntityBase {

	@Column(name = "product_schedule_id", nullable = false)
	private UUID productScheduleId;

	@Column(name = "user_id", nullable = false)
	private UUID userId;

	@Column(name = "guest_count", nullable = false)
	private int guestCount;

	@Column(name = "restore_status")
	private int restoreStatus;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 50)
	private ReservationStatus status;

	@PrePersist
	public void prePersist() {
		if (this.status == null) {
			this.status = ReservationStatus.RESERVED;
		}

		this.restoreStatus = 0;
	}

	public void changeRestoreStatus(int restoreStatus) {
		this.restoreStatus = restoreStatus;
	}

	public void changeStatus(ReservationStatus status) {
		this.status = status;
	}
}
