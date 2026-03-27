package jabaclass.product.domain.model;

import java.util.UUID;

import org.hibernate.annotations.UuidGenerator;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jabaclass.product.domain.model.status.OrderStatus;
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

@Entity
@Getter
@NoArgsConstructor
@SuperBuilder
@EntityListeners(AuditingEntityListener.class)
@Table(name = "products_users", schema = "public")
public class ProductUser extends EntityBase {

	@Id
	@UuidGenerator
	@Column(name = "id", updatable = false, nullable = false)
	private UUID id;

	@Column(name = "product_schedule_id", nullable = false)
	private UUID productScheduleId;

	@Column(name = "user_id", nullable = false)
	private UUID userId;

	@Column(name = "guest_count", nullable = false)
	private int guestCount;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 50)
	private OrderStatus status;

	@PrePersist
	public void prePersist() {
		if (this.status == null) {
			this.status = OrderStatus.PENDING;
		}
	}

	public void changeStatus(OrderStatus status) {
		this.status = status;
	}
}
