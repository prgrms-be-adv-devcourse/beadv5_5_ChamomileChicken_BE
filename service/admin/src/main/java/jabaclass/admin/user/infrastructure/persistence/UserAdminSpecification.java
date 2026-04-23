package jabaclass.admin.user.infrastructure.persistence;

import org.springframework.data.jpa.domain.Specification;

import jabaclass.admin.common.error.AdminErrorCode;
import jabaclass.admin.common.error.BusinessException;
import jabaclass.admin.user.domain.dto.UserSearchCondition;
import jabaclass.admin.user.domain.model.User;
import jabaclass.admin.user.domain.model.UserRole;

public class UserAdminSpecification {

	private UserAdminSpecification() {
	}

	public static Specification<User> withCondition(UserSearchCondition condition) {
		Specification<User> spec = Specification.where(
			(root, query, cb) -> cb.conjunction()
		);
		spec = spec.and(hasRole(condition.role()));
		spec = spec.and(nameLike(condition.name()));
		spec = spec.and(emailLike(condition.email()));
		return spec;
	}

	private static Specification<User> hasRole(String role) {
		if (role == null || role.isBlank()) {
			return (root, query, cb) -> cb.conjunction();
		}
		try {
			UserRole userRole = UserRole.valueOf(role);
			return (root, query, cb) -> cb.equal(root.get("role"), userRole);
		} catch (IllegalArgumentException e) {
			throw new BusinessException(AdminErrorCode.INVALID_ROLE_VALUE);
		}
	}

	private static Specification<User> nameLike(String name) {
		if (name == null || name.isBlank()) {
			return (root, query, cb) -> cb.conjunction();
		}
		String escaped = name.replace("%", "\\%").replace("_", "\\_");
		return (root, query, cb) -> cb.like(root.get("name"), "%" + escaped + "%");
	}

	private static Specification<User> emailLike(String email) {
		if (email == null || email.isBlank()) {
			return (root, query, cb) -> cb.conjunction();
		}
		String escaped = email.replace("%", "\\%").replace("_", "\\_");
		return (root, query, cb) -> cb.like(root.get("email"), "%" + escaped + "%");
	}
}
