package jabaclass.order.domain.model;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

import jabaclass.order.application.exception.OrderErrorCode;
import jabaclass.order.common.error.BusinessException;
import jabaclass.order.common.model.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;

@Entity
@Table(name = "orders")
@Getter
public class Order extends BaseEntity {

	@Column(name = "product_schedule_id", nullable = false)
	private UUID productScheduleId;

	@Column(name = "seller_id", nullable = false)
	private UUID sellerId;

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
		UUID productScheduleId,
		UUID sellerId,
		UUID userId,
		UUID productUserId,
		Integer quantity,
		BigDecimal price,
		OrderStatus status
	) {
		this.productScheduleId = productScheduleId;
		this.sellerId = sellerId;
		this.userId = userId;
		this.productUserId = productUserId;
		this.quantity = quantity;
		this.price = price;
		this.status = status;
	}

	public static Order create(
		UUID productScheduleId,
		UUID sellerId,
		UUID userId,
		UUID productUserId,
		Integer quantity,
		BigDecimal price
	) {
		validateQuantity(quantity);
		validateSellerId(sellerId);
		validateProductUserId(productUserId);
		validatePrice(price);

		return new Order(
			productScheduleId,
			sellerId,
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

	public void pay() {
		if (this.status != OrderStatus.PENDING) {
			throw new BusinessException(OrderErrorCode.ORDER_PAY_NOT_ALLOWED);
		}
		this.status = OrderStatus.PAID;
	}

	public void failPayment() {
		if (this.status != OrderStatus.PENDING) {
			throw new BusinessException(OrderErrorCode.ORDER_FAIL_PAYMENT_NOT_ALLOWED);
		}
		this.status = OrderStatus.FAILED;
	}

	public void refund() {
		if (this.status != OrderStatus.PAID) {
			throw new BusinessException(OrderErrorCode.ORDER_REFUND_NOT_ALLOWED);
		}
		this.status = OrderStatus.REFUNDED;
	}

	public boolean isPaymentAmountValid(BigDecimal amount) {
		if (Objects.isNull(amount)) {
			return false;
		}
		return price.compareTo(amount) == 0;
	}

	public void expire() {
		if (this.status != OrderStatus.PENDING) {
			throw new BusinessException(OrderErrorCode.ORDER_EXPIRE_NOT_ALLOWED);
		}
		this.status = OrderStatus.EXPIRED;
	}

	private static void validateQuantity(Integer quantity) {
		if (Objects.isNull(quantity) || quantity <= 0) {
			throw new BusinessException(OrderErrorCode.ORDER_QUANTITY_INVALID);
		}
	}

	private static void validateProductUserId(UUID productUserId) {
		if (Objects.isNull(productUserId)) {
			throw new BusinessException(OrderErrorCode.ORDER_PRODUCT_USER_ID_REQUIRED);
		}
	}

	private static void validateSellerId(UUID sellerId) {
		if (Objects.isNull(sellerId)) {
			throw new BusinessException(OrderErrorCode.ORDER_PRODUCT_NOT_AVAILABLE);
		}
	}

	private static void validatePrice(BigDecimal price) {
		if (Objects.isNull(price) || price.signum() < 0) {
			throw new BusinessException(OrderErrorCode.ORDER_PRICE_INVALID);
		}
	}
}
