package jabaclass.admin.settlement.infrastructure.persistence;

import java.util.UUID;

import org.springframework.data.jpa.domain.Specification;

import jabaclass.admin.common.error.AdminErrorCode;
import jabaclass.admin.common.error.BusinessException;
import jabaclass.admin.settlement.domain.dto.SettlementSearchCondition;
import jabaclass.admin.settlement.domain.model.Settlement;
import jabaclass.admin.settlement.domain.model.SettlementStatus;

public class SettlementAdminSpecification {

	private SettlementAdminSpecification() {
	}

	public static Specification<Settlement> withCondition(SettlementSearchCondition condition) {
		Specification<Settlement> spec = Specification.where(
			(root, query, cb) -> cb.conjunction()
		);
		spec = spec.and(hasStatus(condition.status()));
		spec = spec.and(hasSellerId(condition.sellerId()));
		spec = spec.and(hasSettlementMonth(condition.settlementMonth()));
		return spec;
	}

	private static Specification<Settlement> hasStatus(String status) {
		if (status == null || status.isBlank()) {
			return (root, query, cb) -> cb.conjunction();
		}
		try {
			SettlementStatus settlementStatus = SettlementStatus.valueOf(status);
			return (root, query, cb) -> cb.equal(root.get("status"), settlementStatus);
		} catch (IllegalArgumentException e) {
			throw new BusinessException(AdminErrorCode.INVALID_STATUS_VALUE);
		}
	}

	private static Specification<Settlement> hasSellerId(UUID sellerId) {
		if (sellerId == null) {
			return (root, query, cb) -> cb.conjunction();
		}
		return (root, query, cb) -> cb.equal(root.get("sellerId"), sellerId);
	}

	private static Specification<Settlement> hasSettlementMonth(String settlementMonth) {
		if (settlementMonth == null || settlementMonth.isBlank()) {
			return (root, query, cb) -> cb.conjunction();
		}
		return (root, query, cb) -> cb.equal(root.get("settlementMonth"), settlementMonth);
	}
}
