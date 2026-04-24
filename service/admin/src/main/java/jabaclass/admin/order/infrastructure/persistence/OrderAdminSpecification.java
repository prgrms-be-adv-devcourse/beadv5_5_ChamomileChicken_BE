package jabaclass.admin.order.infrastructure.persistence;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.data.jpa.domain.Specification;

import jabaclass.admin.common.error.AdminErrorCode;
import jabaclass.admin.common.error.BusinessException;
import jabaclass.admin.order.domain.dto.OrderSearchCondition;
import jabaclass.admin.order.domain.model.Order;
import jabaclass.admin.order.domain.model.OrderStatus;

public class OrderAdminSpecification {

	private OrderAdminSpecification() {
	}

	public static Specification<Order> withCondition(OrderSearchCondition condition) {
		Specification<Order> spec = Specification.where(
			(root, query, cb) -> cb.conjunction()
		);
		spec = spec.and(hasStatus(condition.status()));
		spec = spec.and(hasSellerId(condition.sellerId()));
		spec = spec.and(createdAfter(condition.startDate()));
		spec = spec.and(createdBefore(condition.endDate()));
		return spec;
	}

	private static Specification<Order> hasStatus(String status) {
		if (status == null || status.isBlank()) {
			return (root, query, cb) -> cb.conjunction();
		}
		try {
			OrderStatus orderStatus = OrderStatus.valueOf(status);
			return (root, query, cb) -> cb.equal(root.get("status"), orderStatus);
		} catch (IllegalArgumentException e) {
			throw new BusinessException(AdminErrorCode.INVALID_STATUS_VALUE);
		}
	}

	private static Specification<Order> hasSellerId(UUID sellerId) {
		if (sellerId == null) {
			return (root, query, cb) -> cb.conjunction();
		}
		return (root, query, cb) -> cb.equal(root.get("sellerId"), sellerId);
	}

	private static Specification<Order> createdAfter(LocalDateTime startDate) {
		if (startDate == null) {
			return (root, query, cb) -> cb.conjunction();
		}
		return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("createdAt"), startDate);
	}

	private static Specification<Order> createdBefore(LocalDateTime endDate) {
		if (endDate == null) {
			return (root, query, cb) -> cb.conjunction();
		}
		return (root, query, cb) -> cb.lessThanOrEqualTo(root.get("createdAt"), endDate);
	}
}
