package jabaclass.order.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "orders")
@Getter
public class Order {

	@Id
	@Column(name = "id", nullable = false, updatable = false)
	private UUID id;

	@Column(name = "product_schedule_id", nullable = false)
	private UUID productScheduleId;

	@Column(name = "user_id", nullable = false)
	private UUID userId;

	@Column(name = "quantity", nullable = false)
	private Integer quantity;

	@Column(name = "price", nullable = false, precision = 10, scale = 2)
	private BigDecimal price;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 20)
	private OrderStatus status;

	protected Order() {
	}

	private Order(UUID id,UUID productScheduleId,UUID userId,Integer quantity,
		BigDecimal price, OrderStatus status) {
		this.id = id;
		this.productScheduleId = productScheduleId;
		this.userId = userId;
		this.quantity = quantity;
		this.price = price;
		this.status = status;
	}

	public static Order create(UUID productScheduleId, UUID userId, Integer quantity, BigDecimal price) {
		validateQuantity(quantity);
		validatePrice(price);

		return new Order(UUID.randomUUID(), productScheduleId, userId, quantity, price, OrderStatus.PENDING);
	}

	private static void validateQuantity(Integer quantity) {
		if (Objects.isNull(quantity) || quantity <= 0) {
			throw new IllegalArgumentException("수량은 1 이상이어야 합니다.");
		}
	}

	private static void validatePrice(BigDecimal price) {
		if (Objects.isNull(price) || price.signum() < 0) {
			throw new IllegalArgumentException("주문 가격은 0 이상이어야 합니다.");
		}
	}
}
