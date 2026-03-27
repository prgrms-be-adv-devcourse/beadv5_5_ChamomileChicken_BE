package jabaclass.order.order.domain.model;

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

	@Column(name = "product_user_id")
	private UUID productUserId;

	@Column(name = "quantity", nullable = false)
	private Integer quantity;

	@Column(name = "price", nullable = false, precision = 10, scale = 2)
	private BigDecimal price;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 20)
	private OrderStatus status;

	protected Order() {
	}

	private Order(
		UUID id,
		UUID productScheduleId,
		UUID userId,
		UUID productUserId,
		Integer quantity,
		BigDecimal price,
		OrderStatus status
	) {
		this.id = id;
		this.productScheduleId = productScheduleId;
		this.userId = userId;
		this.productUserId = productUserId;
		this.quantity = quantity;
		this.price = price;
		this.status = status;
	}

	public static Order create(
		UUID productScheduleId,
		UUID userId,
		UUID productUserId,
		Integer quantity,
		BigDecimal price
	) {
		validateQuantity(quantity);
		validateProductUserId(productUserId);
		validatePrice(price);

		return new Order(
			UUID.randomUUID(),
			productScheduleId,
			userId,
			productUserId,
			quantity,
			price,
			OrderStatus.PENDING
		);
	}

	public boolean isOwnedBy(UUID userId) {
		return this.userId.equals(userId);
	}

	public boolean canCancel() {
		return (status == OrderStatus.PENDING) || (status == OrderStatus.PAID);
	}

	public void cancel() {
		this.status = OrderStatus.CANCELLED;
	}

	public void pay() {
		this.status = OrderStatus.PAID;
	}

	public void failPayment() {
		this.status = OrderStatus.FAILED;
	}

	public boolean isPaymentAmountValid(BigDecimal amount) {
		if (Objects.isNull(amount)) {
			return false;
		}

		return price.compareTo(amount) == 0;
	}

	private static void validateQuantity(Integer quantity) {
		if (Objects.isNull(quantity) || quantity <= 0) {
			throw new IllegalArgumentException("수량은 1 이상이어야 합니다.");
		}
	}

	private static void validateProductUserId(UUID productUserId) {
		if (Objects.isNull(productUserId)) {
			throw new IllegalArgumentException("상품 소유자 ID는 비어 있을 수 없습니다.");
		}
	}

	private static void validatePrice(BigDecimal price) {
		if (Objects.isNull(price) || price.signum() < 0) {
			throw new IllegalArgumentException("주문 가격은 0 이상이어야 합니다.");
		}
	}
}
