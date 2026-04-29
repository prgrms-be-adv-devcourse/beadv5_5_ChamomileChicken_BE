package jabaclass.user.auth.domain.model;

import java.util.UUID;
import java.time.LocalDateTime;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "token_blacklist")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TokenBlacklist {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@Column(name = "id", updatable = false, nullable = false)
	private UUID id;

	@Column(name = "token_hash", nullable = false, length = 64, unique = true)
	private String tokenHash;

	@Column(name = "expires_at", nullable = false)
	private LocalDateTime expiresAt;

	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;

	@PrePersist
	void prePersist() {
		this.createdAt = LocalDateTime.now();
	}

	public static TokenBlacklist of(String tokenHash, LocalDateTime expiresAt) {
		TokenBlacklist t = new TokenBlacklist();
		t.tokenHash = tokenHash;
		t.expiresAt = expiresAt;
		return t;
	}
}
