package jabaclass.user.user.domain.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jabaclass.user.common.model.BaseEntity;
import jabaclass.user.deposit.domain.exception.DepositErrorCode;
import jabaclass.user.deposit.domain.exception.DepositException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Getter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Entity
@Table(name = "users", uniqueConstraints = {
	@UniqueConstraint(name = "uk_users_email_social_type", columnNames = {"email", "social_type"})
})
public class User extends BaseEntity {

	@Column(name = "name", nullable = false, length = 50)
	private String name;

	@Column(name = "email", nullable = false, length = 320)
	private String email;

	@Column(name = "password", length = 255)
	private String password;

	@Column(name = "phone", length = 20)
	private String phone;

	@Enumerated(EnumType.STRING)
	@Column(name = "role", nullable = false, length = 20)
	private UserRole role;

	@Enumerated(EnumType.STRING)
	@Column(name = "social_type", length = 20)
	private SocialType socialType;

	@Column(name = "social_id", length = 255)
	private String socialId;

	@Version
	@Column(name = "version")
	private Long version;

	@Builder.Default
	@Column(name = "deposit", nullable = false, precision = 19, scale = 2)
	private BigDecimal deposit = BigDecimal.ZERO;

	@Column(name = "refresh_token", length = 512)
	private String refreshToken;

	@Column(name = "last_login_ip", length = 45)
	private String lastLoginIp;

	@Column(name = "last_login_user_agent", length = 512)
	private String lastLoginUserAgent;

	@Column(name = "force_logout_at")
	private LocalDateTime forceLogoutAt;

	public void updateProfile(String name, String phone) {
		this.name = name;
		this.phone = phone;
	}

	public void changeEmail(String email) {
		this.email = email;
	}

	public void chargeDeposit(BigDecimal amount) {
		this.deposit = this.deposit.add(amount);
	}

	public void deductDeposit(BigDecimal amount) {
		if (this.deposit.compareTo(amount) < 0) {
			throw new DepositException(DepositErrorCode.INSUFFICIENT_DEPOSIT);
		}

		this.deposit = this.deposit.subtract(amount);
	}

    public void updateRefreshToken(String refreshToken) {
		this.refreshToken = refreshToken;
    }

	public void updateLastLogin(String ip, String userAgent) {
		this.lastLoginIp = ip;
		this.lastLoginUserAgent = userAgent;
	}
}
