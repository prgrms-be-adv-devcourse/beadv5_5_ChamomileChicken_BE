package jabaclass.admin.product.infrastructure.persistence;

import java.util.UUID;

import org.springframework.data.jpa.domain.Specification;

import jabaclass.admin.common.error.AdminErrorCode;
import jabaclass.admin.common.error.BusinessException;
import jabaclass.admin.product.domain.dto.ProductSearchCondition;
import jabaclass.admin.product.domain.model.Product;
import jabaclass.admin.product.domain.model.ProductStatus;

public class ProductAdminSpecification {

	private ProductAdminSpecification() {
	}

	public static Specification<Product> withCondition(ProductSearchCondition condition) {
		Specification<Product> spec = Specification.where(
			(root, query, cb) -> cb.conjunction()
		);
		spec = spec.and(hasStatus(condition.status()));
		spec = spec.and(hasSellerId(condition.sellerId()));
		spec = spec.and(titleLike(condition.title()));
		return spec;
	}

	private static Specification<Product> hasStatus(String status) {
		if (status == null || status.isBlank()) {
			return (root, query, cb) -> cb.conjunction();
		}
		try {
			ProductStatus productStatus = ProductStatus.valueOf(status);
			return (root, query, cb) -> cb.equal(root.get("status"), productStatus);
		} catch (IllegalArgumentException e) {
			throw new BusinessException(AdminErrorCode.INVALID_STATUS_VALUE);
		}
	}

	private static Specification<Product> hasSellerId(UUID sellerId) {
		if (sellerId == null) {
			return (root, query, cb) -> cb.conjunction();
		}
		return (root, query, cb) -> cb.equal(root.get("sellerId"), sellerId);
	}

	private static Specification<Product> titleLike(String title) {
		if (title == null || title.isBlank()) {
			return (root, query, cb) -> cb.conjunction();
		}
		String escaped = title.replace("%", "\\%").replace("_", "\\_");
		return (root, query, cb) -> cb.like(root.get("title"), "%" + escaped + "%");
	}
}
