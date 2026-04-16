package jabaclass.admin.user.domain.model;

import jabaclass.admin.common.error.AdminErrorCode;
import jabaclass.admin.common.error.BusinessException;
import jabaclass.admin.common.model.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Getter
@SuperBuilder
@NoArgsConstructor
@Entity
@Table(name = "users")
public class User extends BaseEntity {

	@Column(name = "name", nullable = false, length = 50)
	private String name;

	@Column(name = "email", nullable = false, unique = true, length = 320)
	private String email;

	@Column(name = "phone", nullable = false, length = 20)
	private String phone;

	@Enumerated(EnumType.STRING)
	@Column(name = "role", nullable = false, length = 20)
	private UserRole role;

	public void approveSeller() {
		if (this.role != UserRole.USER) {
			throw new BusinessException(AdminErrorCode.INVALID_SELLER_APPROVAL);
		}
		this.role = UserRole.SELLER;
	}
}
